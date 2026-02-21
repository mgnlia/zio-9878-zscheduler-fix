# Checkpoint 2 — Minimal Fix + Regression Hardening

Task: `0CunfAUcPE6OhGlb1QPXW` (zio/zio#9878)

## What changed

Implemented a minimal deterministic unpark-throttling policy in:

- `src/main/scala/zio/internal/SchedulerUnparkPolicy.scala`

### Policy behavior

- Keeps existing safety gates:
  - unpark only if `queuedTasks > 0`
  - unpark only if `(activeWorkers + searchingWorkers) < poolSize`
- Adds submit-count throttling:
  - default `minSubmissionsBetweenUnparks = 20`
- Adds pressure-aware relaxation:
  - higher queue pressure lowers effective throttle gap (`20 -> 10 -> 5` with default settings)

This directly addresses the issue report concern that unpark is expensive and currently triggered too often in hot paths.

## Regression suite hardening

Expanded tests in:

- `src/test/scala/zio/internal/SchedulerUnparkPolicyRegressionSpec.scala`

Coverage now includes:

1. sustained low-pressure throttling expectation (`48` unparks over `1000` submits)
2. no queued work => no unparks
3. at pool capacity => no unparks
4. high pressure relaxes throttle vs low pressure
5. explicit before/after churn comparison (eager vs throttled)

## Deterministic before/after metrics (model)

Scenario: `poolSize=8`, `activeWorkers=3`, `searchingWorkers=0`, 1000 submit decisions.

- **Before (eager):** `1000` unparks / `1000` submits
- **After (throttled, low pressure `queuedTasks=1`):** `48` unparks / `1000` submits
- **After (throttled, high pressure `queuedTasks=8`):** `167` unparks / `1000` submits

Interpretation:

- Low-pressure churn reduced by ~95.2% (`1000 -> 48`)
- High-pressure path still accelerates unparks compared with low-pressure path (`167 > 48`), preserving responsiveness under load
