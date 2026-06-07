# Security Remediation Proposal

## Context
A security audit of `AndroidManifest.xml` and `firestore.rules` revealed that while the Android app correctly implements least privilege, the Firestore rules have significant vulnerabilities. The most critical is a Privilege Escalation flaw that allows newly authenticated users to assign themselves `ADMIN` or `DUEÑO` roles.

## Proposed Solutions

### 1. Fix Privilege Escalation in `v2_users`
We must strictly control role assignment. 
- **Option A**: Force new users to only create their document with the `VENDEDOR` (or `PENDIENTE`) role.
- **Option B (Preferred)**: Only allow `esAdmin()` to assign roles other than `VENDEDOR`.
```javascript
// Proposed Rule:
allow create: if esAutenticado() && request.auth.uid == uid
              && request.resource.data.keys().hasAll(['uid', 'nombre', 'correo', 'rol'])
              && (
                request.resource.data.rol == 'VENDEDOR' || 
                (esAdmin() && request.resource.data.rol in ['VENDEDOR', 'ADMIN', 'DUEÑO'])
              );
```

### 2. Prevent ID Spoofing
Enforce that action-based documents correspond to the authenticated user creating them.
- For `v2_solicitudes`, add `&& request.resource.data.empleadoId == request.auth.uid`.
- For `v2_ventas`, add `&& request.resource.data.atendio == request.auth.uid`.

### 3. Prevent Schema Pollution
Replace `hasAll` with `hasOnly` where practical, or explicitly define allowed optional fields.
- For `v2_ventas`, enforce a `hasOnly` check with all permitted fields (e.g., `['id', 'total', 'fecha', 'sucursal', 'atendio', 'articulos', 'metodoPago', '...']`). 
- For `v2_turnos_caja`, define a strict schema array for the `abierto` state.
- For `v2_gastos`, define strict schema limits.

## Impact
These changes will harden the backend against malicious actors trying to exploit the client APIs, ensuring that valid Vendedores can only perform authorized actions under their own identity, and completely mitigating the privilege escalation vector. No changes are required in `AndroidManifest.xml`.
