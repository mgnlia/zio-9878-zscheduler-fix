package zio.internal

import munit.FunSuite

final class SchedulerUnparkPolicyRegressionSpec extends FunSuite {

  test("regression: sustained low-pressure submits are throttled") {
    val policy  = new SchedulerUnparkPolicy(poolSize = 8)
    val unparks = (1 to 1000).count(_ => policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))

    // Deterministic expectation with default minSubmissionsBetweenUnparks=20.
    assertEquals(unparks, 48)
    assert(unparks <= 50, clues(s"unparks=$unparks"))
  }

  test("no queued work -> no unpark") {
    val policy  = new SchedulerUnparkPolicy(poolSize = 8)
    val unparks = (1 to 200).count(_ => policy.shouldUnpark(activeWorkers = 1, searchingWorkers = 0, queuedTasks = 0))

    assertEquals(unparks, 0)
  }

  test("at pool capacity -> no unpark") {
    val policy  = new SchedulerUnparkPolicy(poolSize = 8)
    val unparks = (1 to 200).count(_ => policy.shouldUnpark(activeWorkers = 8, searchingWorkers = 0, queuedTasks = 10))

    assertEquals(unparks, 0)
  }

  test("high pressure relaxes throttle versus low pressure") {
    val policyLow  = new SchedulerUnparkPolicy(poolSize = 8)
    val policyHigh = new SchedulerUnparkPolicy(poolSize = 8)

    val lowPressureUnparks =
      (1 to 1000).count(_ => policyLow.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))

    val highPressureUnparks =
      (1 to 1000).count(_ => policyHigh.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 8))

    assert(highPressureUnparks > lowPressureUnparks, clues(s"low=$lowPressureUnparks high=$highPressureUnparks"))
  }

  test("before/after model comparison: throttled policy cuts unpark churn") {
    def eagerShouldUnpark(poolSize: Int, activeWorkers: Int, searchingWorkers: Int, queuedTasks: Int): Boolean =
      queuedTasks > 0 && (activeWorkers + searchingWorkers) < poolSize

    val policy = new SchedulerUnparkPolicy(poolSize = 8)

    val eagerUnparks = (1 to 1000).count(_ => eagerShouldUnpark(poolSize = 8, activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))
    val fixedUnparks = (1 to 1000).count(_ => policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))

    assertEquals(eagerUnparks, 1000)
    assertEquals(fixedUnparks, 48)
    assert(fixedUnparks < eagerUnparks / 10, clues(s"eager=$eagerUnparks fixed=$fixedUnparks"))
  }

  test("validation: invalid constructor args are rejected") {
    intercept[IllegalArgumentException](new SchedulerUnparkPolicy(poolSize = 0))
    intercept[IllegalArgumentException](new SchedulerUnparkPolicy(poolSize = 8, minSubmissionsBetweenUnparks = -1))
  }
}
