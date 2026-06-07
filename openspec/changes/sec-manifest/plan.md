# Security Remediation Plan

## Phase 1: Preparation
1. Review `firestore.rules` current schema dependencies for `v2_ventas`, `v2_turnos_caja`, `v2_gastos`, and `v2_jornadas`.
2. Extract the exact required and optional fields used by the client app for these collections to build accurate `hasOnly` lists.

## Phase 2: Rules Modification
1. **Patch `v2_users`**:
   - Update `allow create` to enforce `rol == 'VENDEDOR'` unless the creator `esAdmin()`.
2. **Patch ID Spoofing**:
   - In `v2_solicitudes`, append `&& request.resource.data.empleadoId == request.auth.uid`.
   - In `v2_ventas`, append `&& request.resource.data.atendio == request.auth.uid` (or verify if there are valid admin override cases).
3. **Apply Strict Schemas (`hasOnly`)**:
   - Create helper functions for schemas if they are large (e.g., `ventaFields()`).
   - Replace `hasAll` with `hasOnly` in `v2_ventas`.
   - Replace `hasAll` with `hasOnly` in `v2_gastos`.
   - Update `v2_turnos_caja` to use `hasOnly` for the `estado == 'abierto'` case.

## Phase 3: Testing & Verification
1. Run Firebase local emulator tests or deploy to a staging project.
2. Verify normal POS checkout succeeds.
3. Verify new user creation assigns `VENDEDOR` by default.
4. Verify ID spoofing is rejected.
5. Do not `git commit` until all rules are thoroughly tested and proven not to break the offline/online counter sync functionality (`SyncWorker`).
