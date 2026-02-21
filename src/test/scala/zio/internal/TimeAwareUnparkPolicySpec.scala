package zio.internal

import munit.FunSuite

final class TimeAwareUnparkPolicySpec extends FunSuite {

  private final class FakeClock(var nowNanos: Long) {
    def now(): Long = nowNanos
    def advance(deltaNanos: Long): Unit = nowNanos += deltaNanos
  }

  test("time throttle: repeated submits at same time do not unpark repeatedly") {
    val clock  = new FakeClock(0L)
    val policy = new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = 100L, nanoTime = () => clock.now())

    // active=7 => deficit=1, so no deficit-tier relaxation in this test.
    val decisions = (1 to 100).map(_ => policy.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))

    assertEquals(decisions.count(identity), 1)
  }

  test("time throttle: unpark allowed again after required gap") {
    val clock  = new FakeClock(0L)
    val policy = new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = 100L, nanoTime = () => clock.now())

    assert(policy.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))
    assert(!policy.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))

    clock.advance(99L)
    assert(!policy.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))

    clock.advance(1L)
    assert(policy.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))
  }

  test("pressure/deficit relaxation lowers effective gap") {
    val lowClock  = new FakeClock(0L)
    val highClock = new FakeClock(0L)

    val low = new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = 80L, nanoTime = () => lowClock.now())
    val high = new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = 80L, nanoTime = () => highClock.now())

    assert(low.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))
    assert(high.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 8))

    // low pressure requires full base gap; high pressure/deficit allows earlier unparks.
    lowClock.advance(20L)
    highClock.advance(20L)

    assert(!low.shouldUnpark(activeWorkers = 7, searchingWorkers = 0, queuedTasks = 1))
    assert(high.shouldUnpark(activeWorkers = 1, searchingWorkers = 0, queuedTasks = 8))
  }

  test("starvation bypass: no active workers with queue should unpark immediately") {
    val clock  = new FakeClock(0L)
    val policy = new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = 1000L, nanoTime = () => clock.now())

    assert(policy.shouldUnpark(activeWorkers = 0, searchingWorkers = 0, queuedTasks = 1))
    assert(policy.shouldUnpark(activeWorkers = 0, searchingWorkers = 0, queuedTasks = 1))
  }

  test("safety guards: no queued tasks or at capacity do not unpark") {
    val clock  = new FakeClock(0L)
    val policy = new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = 100L, nanoTime = () => clock.now())

    assert(!policy.shouldUnpark(activeWorkers = 3, searchingWorkers = 0, queuedTasks = 0))
    assert(!policy.shouldUnpark(activeWorkers = 8, searchingWorkers = 0, queuedTasks = 10))
  }

  test("validation: invalid constructor args are rejected") {
    intercept[IllegalArgumentException](new TimeAwareUnparkPolicy(poolSize = 0))
    intercept[IllegalArgumentException](new TimeAwareUnparkPolicy(poolSize = 8, minNanosBetweenUnparks = -1L))
  }
}
