# Verification Packet — 0Cun (Critical Reopen Update)

Task: `0CunfAUcPE6OhGlb1QPXW`  
Issue target: https://github.com/zio/zio/issues/9878

---

## 1) Passing CI run URL (required)

- ✅ Passing CI run: https://github.com/mgnlia/zio-9878-zscheduler-fix/actions/runs/22248759244
- Workflow: `CI`
- Conclusion: `success`
- Commit: `308a62ccbddea616a0621c2d7546239548c922a5`

---

## 2) Upstream PR URL against zio/zio for #9878 (required)

- ✅ Upstream PR URL: https://github.com/zio/zio/pull/10440
- PR title: `fix: reduce ZScheduler unpark frequency via batching (#9878)`
- Repository: `zio/zio`
- Notes: PR is directly against upstream and explicitly references/closes `#9878`.

---

## 3) Concise metrics evidence (required)

### A. Upstream PR metrics evidence
Source URL: https://github.com/zio/zio/pull/10440

Reported in PR body:
- Before: ~100 unpark calls / 100 submits
- After: ~13 unpark calls / 100 submits
- Reduction: **87.5%**

### B. Local deterministic model evidence
Source URL: https://github.com/mgnlia/zio-9878-zscheduler-fix/blob/main/CHECKPOINT_2_IMPLEMENTATION.md

Reported in checkpoint:
- Before (eager): `1000` unparks / `1000` submits
- After (throttled, low pressure): `48` unparks / `1000` submits (~95.2% reduction)
- After (throttled, high pressure): `167` unparks / `1000` submits

---

## 4) Return-to-review readiness

All three required URL-backed artifacts are now present:
1. Passing CI run URL
2. Upstream PR URL (not issue-only)
3. Updated concise metrics evidence URLs

Recommendation: **RETURN TO REVIEW**.
