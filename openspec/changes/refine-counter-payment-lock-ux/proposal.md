# Proposal

## Objective

Refine the mostrador payment and inactivity lock UI so the AVD-validated checkout remains fast and visually clear.

## Scope

- Lock overlay should use a stable dark scrim independent from custom theme colors.
- Payment sheet should keep exact-cash as the primary fast path.
- Record remaining full redesign work in notes if it is larger than a safe microchange.

## Out of Scope

- No checkout logic changes.
- No inventory or stock deduction changes.
- No offline/sync changes.
- No payment method data model changes.

## Files

- `app/src/main/java/com/bocatta/pos/presentation/ui/components/DialogosVentas.kt`
- `notas.txt`

## Risks

- Payment UI changes can accidentally trigger duplicate checkout; avoid changing `finalizarVenta` call semantics.
- Lock UI must not weaken PIN behavior.
