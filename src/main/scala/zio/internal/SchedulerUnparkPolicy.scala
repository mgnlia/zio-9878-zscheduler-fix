package zio.internal

/**
 * Minimal fix model for zio/zio#9878.
 *
 * The previous eager condition would return `true` on every submit under
 * sustained pressure, causing excessive unpark churn. This policy adds a
 * deterministic submit-count throttle and modest pressure-aware relaxation.
 *
 * NOTE: This is a small deterministic model used for regression work in this
 * repository and is intentionally single-threaded / not synchronized.
 */
final class SchedulerUnparkPolicy(
  poolSize: Int,
  minSubmissionsBetweenUnparks: Int = 20
) {
  require(poolSize > 0, "poolSize must be > 0")
  require(minSubmissionsBetweenUnparks >= 0, "minSubmissionsBetweenUnparks must be >= 0")

  // Start "ready" so the first pressured submit can immediately unpark.
  private[this] var submissionsSinceLastUnpark = Int.MaxValue

  def shouldUnpark(activeWorkers: Int, searchingWorkers: Int, queuedTasks: Int): Boolean = {
    val hasQueuedWork  = queuedTasks > 0
    val belowPoolLimit = (activeWorkers + searchingWorkers) < poolSize

    if (!hasQueuedWork || !belowPoolLimit) false
    else {
      val gap    = effectiveSubmissionGap(queuedTasks)
      val should = submissionsSinceLastUnpark >= gap

      submissionsSinceLastUnpark =
        if (should) 0
        else if (submissionsSinceLastUnpark == Int.MaxValue) Int.MaxValue
        else submissionsSinceLastUnpark + 1

      should
    }
  }

  private def effectiveSubmissionGap(queuedTasks: Int): Int = {
    if (minSubmissionsBetweenUnparks == 0) 0
    else {
      val pressureTier =
        if (queuedTasks >= poolSize) 2
        else if (queuedTasks >= math.max(1, poolSize / 2)) 1
        else 0

      math.max(1, minSubmissionsBetweenUnparks >> pressureTier)
    }
  }
}
