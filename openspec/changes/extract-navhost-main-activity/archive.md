# Archive Report — extract-navhost-main-activity

**Date:** 2026-06-10  
**Status:** ✅ ARCHIVED

---

## Change Summary

Extracted the 19-route NavHost block from MainActivity (338 lines → 76) into a dedicated `AppNavGraph.kt` composable at `com.bocatta.pos.navigation`. Pure structural extraction — zero behavioral changes.

## Artifacts

| Artifact | File |
|----------|------|
| Proposal | `openspec/changes/extract-navhost-main-activity/proposal.md` |
| Spec | `openspec/changes/extract-navhost-main-activity/spec.md` |
| Design | `openspec/changes/extract-navhost-main-activity/design.md` |
| Tasks | `openspec/changes/extract-navhost-main-activity/tasks.md` |
| Verification | `openspec/changes/extract-navhost-main-activity/verification.md` |

## Key Outcomes

- **MainActivity.kt:** 338 → 76 lines (−77%)
- **AppNavGraph.kt (new):** 261 lines with all 19 routes
- **Deviation from design:** Pass ViewModel instances instead of 22 individual callback lambdas — simpler API, matches original usage
- **Build:** ✅ | **Tests:** 318 total, 316 passed (+1 new test), 2 pre-existing failures
- **Net code change:** ~28 lines (261 new + 29 test − 262 removed)

## Open Items

1. AdminScreen.kt still uses string-based `seccionActiva` dispatch instead of `AdminContent()` — future refactor
2. Write instrumentation tests for AppNavGraph using `createComposeRule` (requires androidTest dependencies)
