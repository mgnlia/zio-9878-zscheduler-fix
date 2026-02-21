# zio-9878-zscheduler-fix

Execution workspace for **zio/zio#9878** ("ZScheduler parks+unparks workers too frequently").

## Checkpoint 1
- Added a deterministic reproduction model for eager unpark decisions.
- Added a regression test that intentionally fails against the current eager behavior.
- Prepared project skeleton for iterative fix implementation.
