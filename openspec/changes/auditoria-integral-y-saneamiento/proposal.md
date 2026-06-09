# Proposal: Auditoría Integral y Saneamiento

## Intent

Transition the project from "Alpha" to "Beta" state by sanitizing the fragmented SDD history, auditing critical production code that bypassed the design process, and establishing operational gates to prevent future process decay.

## Scope

### In Scope
- **Process Governance**: Implement "Soft Gates" (justification required for 3+ open changes) and auto-archiving policy (14 days inactivity $\rightarrow$ suspended).
- **Technical Audit (Reverse SDD + Correction)**: Document and fix gaps in critical paths: `SalesViewModelV2` (checkout), `InventoryDeductions`, `OfflineManager`, and `SyncWorker`.
- **SDD Saneamiento**: Resolve 18 truncated changes in `openspec/changes/` via completion or `cancel.md` (with justification).
- **Beta Certification**: Full Spec $\rightarrow$ Design $\rightarrow$ Verification cycle for the 6 critical flows.

### Out of Scope
- Development of new product features.
- Refactoring of non-critical UI components.

## Capabilities

### New Capabilities
- `sdd-governance`: Rules for change lifecycle, gates, and archiving.
- `sales-checkout`: Specification of the checkout flow and SalesViewModelV2.
- `inventory-management`: Specification of stock deductions and offline inventory logic.
- `offline-sync`: Specification of the synchronization engine and SyncWorker.
- `caja-management`: Specification of cash register operations.
- `reports-engine`: Specification of sales and operational reporting.

### Modified Capabilities
- None

## Approach

1. **Governance Setup**: Define the "Soft Gate" and archiving rules in the process manual.
2. **Reverse SDD & Fix**: 
   - Analyze current behavior of critical components $\rightarrow$ Document as Spec $\rightarrow$ Identify gaps $\rightarrow$ Apply immediate corrections.
3. **Fragmentation Cleanup**: 
   - Audit each of the 18 truncated changes.
   - If irrelevant $\rightarrow$ Create `cancel.md`.
   - If critical $\rightarrow$ Complete minimal `spec.md` and archive.
4. **Beta Validation**: Run end-to-end verification on the 6 identified critical flows.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/changes/` | Modified | Saneamiento of 18+ folders. |
| `openspec/specs/` | New | Creation of 6 critical flow specifications. |
| `src/.../SalesViewModelV2` | Modified | Corrected checkout logic. |
| `src/.../OfflineManager` | Modified | Improved sync stability. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Regression in critical checkout/sync paths | High | Mandatory offline verification and strict E2E testing. |
| Scope creep during audit | Medium | Stick strictly to the 6 critical flows and listed components. |

## Rollback Plan

- **Code**: Revert changes to critical VMs/Managers via Git.
- **SDD**: Restore `openspec/` directory from the last stable commit before saneamiento.

## Dependencies

- Access to full production codebase for Reverse SDD.

## Success Criteria

- [ ] 18 truncated SDD changes are closed (`archive.md` or `cancel.md`).
- [ ] 6 critical flows have complete Spec $\rightarrow$ Design $\rightarrow$ Verification.
- [ ] Process gates are documented and applied to new changes.
- [ ] Zero critical gaps remaining in `SalesViewModelV2` and `SyncWorker`.
