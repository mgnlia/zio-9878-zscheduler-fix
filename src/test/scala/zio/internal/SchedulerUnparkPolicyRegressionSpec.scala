package zio.internal

import munit.FunSuite

final class SchedulerUnparkPolicyRegressionSpec extends FunSuite {

  test("regression: repeated hot-path submits should not trigger unpark every time") {
    val policy  = new SchedulerUnparkPolicy(poolSize = 8)
    val unparks = (1 to 1000).count(_ => policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))

    // Desired behavior for #9878: rate-limit / batch unparks under sustained pressure.
    // This assertion is expected to FAIL against the current eager strategy.
    assert(unparks <= 50, clues(s"unparks=$unparks"))
  }
}
