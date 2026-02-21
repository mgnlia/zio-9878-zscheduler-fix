package zio.internal

/**
 * Minimal reproduction model for zio/zio#9878.
 *
 * This intentionally mirrors the currently-eager behavior called out in the issue:
 * under persistent pressure, the scheduler keeps deciding to unpark workers,
 * with no temporal rate-limit.
 */
final class SchedulerUnparkPolicy(poolSize: Int) {

  def shouldUnpark(activeWorkers: Int, searchingWorkers: Int, queuedTasks: Int): Boolean =
    queuedTasks > 0 && (activeWorkers + searchingWorkers) < poolSize
}
