package zio.internal

import munit.FunSuite

final class SchedulerUnparkPolicyRegressionSpec extends FunSuite {

  test("regression: sustained low-pressure submits are throttled") {
    val policy  = new SchedulerUnparkPolicy(poolSize = 8)
    val unparks = (1 to 1000).count(_ => policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))

    // Deterministic expectation with default minSubmissionsBetweenUnparks=20.
    assertEquals(unparks, 48)
    assert(unparks <= 50, s"unparks=$unparks")
  }

  test("deterministic pressure tiers: low/medium/high unpark frequencies") {
    val low = new SchedulerUnparkPolicy(poolSize = 8)
    val med = new SchedulerUnparkPolicy(poolSize = 8)
    val hi  = new SchedulerUnparkPolicy(poolSize = 8)

    val lowUnparks = (1 to 1000).count(_ => low.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))
    val medUnparks = (1 to 1000).count(_ => med.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 4))
    val hiUnparks  = (1 to 1000).count(_ => hi.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 8))

    assertEquals(lowUnparks, 48)
    assertEquals(medUnparks, 91)
    assertEquals(hiUnparks, 167)
    assert(lowUnparks < medUnparks && medUnparks < hiUnparks, s"low=$lowUnparks med=$medUnparks hi=$hiUnparks")
  }

  test("eventual progress: under pressure unparks recur within bounded interval") {
    val policy = new SchedulerUnparkPolicy(poolSize = 8, minSubmissionsBetweenUnparks = 20)

    var longestSilentSpan = 0
    var currentSilentSpan = 0

    (1 to 500).foreach { _ =>
      val didUnpark = policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1)
      if (didUnpark) {
        if (currentSilentSpan > longestSilentSpan) longestSilentSpan = currentSilentSpan
        currentSilentSpan = 0
      } else {
        currentSilentSpan += 1
      }
    }

    // With gap=20 in low pressure, silence between unparks should be <= 20 submits.
    assert(longestSilentSpan <= 20, s"longestSilentSpan=$longestSilentSpan")
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

    assert(highPressureUnparks > lowPressureUnparks, s"low=$lowPressureUnparks high=$highPressureUnparks")
  }

  test("before/after model comparison: throttled policy cuts unpark churn") {
    def eagerShouldUnpark(poolSize: Int, activeWorkers: Int, searchingWorkers: Int, queuedTasks: Int): Boolean =
      queuedTasks > 0 && (activeWorkers + searchingWorkers) < poolSize

    val policy = new SchedulerUnparkPolicy(poolSize = 8)

    val eagerUnparks = (1 to 1000).count(_ => eagerShouldUnpark(poolSize = 8, activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))
    val fixedUnparks = (1 to 1000).count(_ => policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 1))

    assertEquals(eagerUnparks, 1000)
    assertEquals(fixedUnparks, 48)
    assert(fixedUnparks < eagerUnparks / 10, s"eager=$eagerUnparks fixed=$fixedUnparks")
  }

  test("validation: invalid constructor args are rejected") {
    intercept[IllegalArgumentException](new SchedulerUnparkPolicy(poolSize = 0))
    intercept[IllegalArgumentException](new SchedulerUnparkPolicy(poolSize = 8, minSubmissionsBetweenUnparks = -1))
  }
}
