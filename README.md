# zio-9878-zscheduler-fix

Execution workspace for **zio/zio#9878** ("ZScheduler parks+unparks workers too frequently").

## Checkpoint 1
- Added a deterministic reproduction model for eager unpark decisions.
- Added a regression test that intentionally fails against the original eager behavior.
- Prepared project skeleton for iterative fix implementation.

## Checkpoint 2
- Implemented minimal deterministic unpark throttling with pressure-aware relaxation.
- Hardened regression coverage for throttle behavior and before/after churn assertions.
- Documented deterministic metrics and PR narrative artifacts.

## Checkpoint 3
- Added an upstream-oriented **time-aware** unpark strategy model.
- Added deterministic tests using a fake clock.
- Documented integration notes for wiring temporal throttling into real `ZScheduler` hot paths.

## Key artifacts
- `CHECKPOINT_1.md`
- `CHECKPOINT_2_IMPLEMENTATION.md`
- `CHECKPOINT_3_TIME_AWARE_STRATEGY.md`
- `PR_DRAFT_9878.md`
