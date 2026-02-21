# Draft PR Narrative — zio/zio#9878

## Title
ZScheduler: reduce hot-path unpark churn via throttled/pressure-aware unpark policy

## Context
Issue: <https://github.com/zio/zio/issues/9878>

`LockSupport.unpark(worker)` is relatively expensive and currently triggered too frequently from scheduler hot paths. The current eager decision strategy can repeatedly unpark under sustained submit pressure.

## Problem
In a deterministic model of current behavior:

- condition: `queuedTasks > 0 && (activeWorkers + searchingWorkers) < poolSize`
- under sustained pressure, this is often true for every submit decision
- result: high park/unpark cycling frequency

## Change summary
Implemented a minimal throttled unpark policy (model-level):

1. Preserve safety gates (must have queued work and worker deficit)
2. Add `minSubmissionsBetweenUnparks` throttle (default `20`)
3. Relax throttle when pressure is high (effective gap reduced by queue-pressure tier)

## Deterministic before/after metrics (model)
Scenario: `poolSize=8`, `active=3`, `searching=0`, 1000 submit decisions.

- **Before (eager):** `1000` unparks
- **After (throttled, low pressure queued=1):** `48` unparks
- **After (throttled, higher pressure queued=8):** `167` unparks

Observed model impact:

- low-pressure unpark churn reduced by ~95.2% (`1000 -> 48`)
- high-pressure decisions still unpark more aggressively than low pressure (`167 > 48`)

## Tests
Regression suite covers:

- sustained low-pressure throttling behavior
- no queued work => no unpark
- pool capacity saturation => no unpark
- pressure-aware relaxation behavior
- explicit before/after churn comparison

## Risk / tradeoff
Main tradeoff is fairness vs unpark aggressiveness:

- throttling may slightly delay wakeups for marginal tasks under low pressure
- pressure-aware relaxation is used to reduce latency risk when queue pressure rises

## Follow-up suggestions
- validate approach against real ZScheduler perf harness on JVM
- tune throttle constants (`minSubmissionsBetweenUnparks`, pressure tiers) with benchmark data
- add instrumentation counters in scheduler internals for production-like workload profiling
