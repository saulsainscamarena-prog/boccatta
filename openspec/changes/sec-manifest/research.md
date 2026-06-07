# Security Audit Research

## 1. AndroidManifest.xml Analysis
The `AndroidManifest.xml` was audited for minimal privilege access and improperly exported components:
- **Permissions**: Uses only `INTERNET` and `ACCESS_NETWORK_STATE`. These are minimal and appropriate for a cloud-connected POS.
- **Exported Components**: Only `MainActivity` is exported (`android:exported="true"`). Since it includes the `MAIN` action and `LAUNCHER` category, it must be exported for the OS to launch the app. No other activities, services, receivers, or providers are present.
- **Conclusion**: The manifest complies with minimal privilege and component security best practices.

## 2. Firebase Rules Analysis (`firestore.rules`)
An audit of `firestore.rules` reveals several severe security vulnerabilities regarding minimal privilege and access control.

### A. Critical Privilege Escalation (`v2_users`)
Any authenticated user can create their own profile. The rule allows them to self-assign any valid role, including `ADMIN` or `DUEÑO`.
```javascript
// Current Vulnerable Rule:
allow create: if esAutenticado() && request.auth.uid == uid
              && request.resource.data.keys().hasAll(['uid', 'nombre', 'correo', 'rol'])
              && request.resource.data.rol in ['VENDEDOR', 'ADMIN', 'DUEÑO'];
```
*Impact*: An attacker can register via Firebase Auth, intercept the creation request, and set `rol: 'ADMIN'`, granting them full access to all collections and system configurations.

### B. ID Spoofing in Operations (`v2_solicitudes`, `v2_ventas`)
Several collections allow authenticated users to submit documents representing actions by employees, but fail to validate that the submitted ID matches their own `request.auth.uid`.
- `v2_solicitudes`: Checks `request.resource.data.empleadoId is string` but does not enforce `request.resource.data.empleadoId == request.auth.uid`.
- `v2_ventas`: Validates schema but doesn't ensure `request.resource.data.atendio` corresponds to the authenticated user.

### C. Arbitrary Field Injection (Schema Pollution)
Many collections use `hasAll` instead of `hasOnly` in `create` or `update` rules. This allows users to inject unexpected, arbitrary fields into the database, violating the Principle of Least Privilege.
Affected collections: `v2_ventas`, `v2_turnos_caja` (when `estado == 'abierto'`), `v2_gastos`, `v2_jornadas`, `v2_auditoria_cancelaciones`.
```javascript
// Example in v2_ventas:
allow create: if esVendedor()
  && request.resource.data.keys().hasAll(['id', 'total', 'fecha', 'sucursal', 'atendio'])
  // ... missing hasOnly or strict bounds
```

## 3. Findings Summary
The Android side (`AndroidManifest.xml`) is solid. However, the `firestore.rules` file needs immediate remediation to prevent privilege escalation, ID spoofing, and schema pollution.
