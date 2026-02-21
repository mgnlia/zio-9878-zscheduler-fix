package zio.internal

/**
 * Strategy model for zio/zio#9878 that uses a time-based throttle.
 *
 * Motivation: `LockSupport.unpark` is expensive in submit hot paths.
 * A submit-count throttle helps, but real scheduler behavior is ultimately
 * temporal. This model introduces a minimal nanosecond gap between unparks,
 * with pressure/deficit-aware relaxation and starvation bypass.
 */
final class TimeAwareUnparkPolicy(
  poolSize: Int,
  minNanosBetweenUnparks: Long = 50_000L,
  nanoTime: () => Long = () => System.nanoTime()
) {
  require(poolSize > 0, "poolSize must be > 0")
  require(minNanosBetweenUnparks >= 0L, "minNanosBetweenUnparks must be >= 0")

  private[this] var lastUnparkAtNanos: Long = Long.MinValue

  def shouldUnpark(activeWorkers: Int, searchingWorkers: Int, queuedTasks: Int): Boolean = {
    val hasQueuedWork  = queuedTasks > 0
    val runningWorkers = activeWorkers + searchingWorkers
    val belowPoolLimit = runningWorkers < poolSize

    if (!hasQueuedWork || !belowPoolLimit) false
    else {
      val deficit    = poolSize - runningWorkers
      val starvation = activeWorkers == 0 && queuedTasks > 0
      val now        = nanoTime()
      val gap        = effectiveGapNanos(queuedTasks = queuedTasks, deficit = deficit)
      val overdue =
        if (lastUnparkAtNanos == Long.MinValue) true
        else (now - lastUnparkAtNanos) >= gap

      val should = starvation || overdue
      if (should) lastUnparkAtNanos = now
      should
    }
  }

  private def effectiveGapNanos(queuedTasks: Int, deficit: Int): Long = {
    if (minNanosBetweenUnparks == 0L) 0L
    else {
      val pressureTier =
        if (queuedTasks >= poolSize) 2
        else if (queuedTasks >= math.max(1, poolSize / 2)) 1
        else 0

      val deficitTier =
        if (deficit >= 4) 1
        else 0

      val shift = math.min(3, pressureTier + deficitTier)
      math.max(1L, minNanosBetweenUnparks >> shift)
    }
  }
}
