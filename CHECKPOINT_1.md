# Checkpoint 1 — Repro + Failing Regression + Branch Status

Task: `0CunfAUcPE6OhGlb1QPXW` (zio/zio#9878)

## 1) Reproduction artifact
Added a minimal deterministic model that mirrors the current eager unpark condition (no temporal guard):

- `src/main/scala/zio/internal/SchedulerUnparkPolicy.scala`

Current behavior in model:

```scala
queuedTasks > 0 && (activeWorkers + searchingWorkers) < poolSize
```

Under sustained pressure (`queuedTasks = 1` in a hot path), this returns `true` every call.

## 2) Failing regression test
Added regression test that encodes expected anti-thrashing behavior:

- `src/test/scala/zio/internal/SchedulerUnparkPolicyRegressionSpec.scala`

The test simulates 1000 hot-path submits and expects capped unparks (`<= 50`).
Given current eager logic, computed `unparks = 1000`, so test fails by design.

## 3) Implementation branch status
- Repository branch used for checkpoint commits: `main`
- Planned implementation branch name for fix iteration: `impl/9878-unpark-throttle`
- Status: repro and failing regression are ready; next step is patching policy (temporal throttle + pressure-aware batching) and then flipping regression green.

## Commits (checkpoint sequence)
- README checkpoint notes: `930e3ba38b0b139fb735a6ef0ad358eea068df0a`
- sbt scaffold: `6dc0ccdba80fbf5463be8a6ef902bb78424c7abd`
- sbt version: `42f75c6f708112b4c6247b6b29451d2fe147fac5`
- gitignore: `40f5a5beccd449b74eb28dc331ecc4fb2df16ef8`
- repro model: `4f292386b39e9aab0344756664a54fc6fdc59f21`
- failing regression: `350ec6c55c9d408f716e8cc4546951b5e44c22f1`
