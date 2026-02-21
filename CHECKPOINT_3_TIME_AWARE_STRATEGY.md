# Checkpoint 3 — Time-aware strategy (upstream-oriented)

Task: `0CunfAUcPE6OhGlb1QPXW` (zio/zio#9878)

## Why this checkpoint

Issue #9878 specifically calls out the cost of `LockSupport.unpark(worker)` in submit hot paths. The prior model fix throttled by *submit count*. This checkpoint adds a strategy model that throttles by *time* to better match the real cost profile.

## Added artifacts

- `src/main/scala/zio/internal/TimeAwareUnparkPolicy.scala`
- `src/test/scala/zio/internal/TimeAwareUnparkPolicySpec.scala`

## Strategy summary

`TimeAwareUnparkPolicy` decides whether to unpark based on:

1. **Safety gates**
   - queued tasks must exist
   - worker deficit must exist (`active + searching < poolSize`)
2. **Temporal throttle**
   - minimum nanos gap between unparks (`minNanosBetweenUnparks`, default `50_000ns` in model)
3. **Pressure + deficit relaxation**
   - high queue pressure and larger worker deficits reduce effective gap
4. **Starvation bypass**
   - if no active workers and queue is non-empty, unpark immediately even if gap not elapsed

## Deterministic behaviors covered by tests

- repeated submit calls at same timestamp do not repeatedly unpark
- unpark permitted when time gap is reached
- high pressure / larger deficit permit earlier unparks than low pressure
- starvation path bypasses throttle to preserve progress
- no-queue and at-capacity guards remain enforced

## Upstream mapping notes

In upstream `ZScheduler`, `maybeUnparkWorker` is currently called from `submit` / `submitAndYield` hot paths. A direct integration candidate is:

- maintain an `AtomicLong lastUnparkNanos`
- before calling `LockSupport.unpark`, enforce elapsed gap with CAS update
- reduce gap under high queue pressure / worker deficit
- keep hard bypass for starvation (`activeWorkers == 0 && queued > 0`)

This aims to reduce expensive unpark churn while retaining responsiveness and eventual progress.
