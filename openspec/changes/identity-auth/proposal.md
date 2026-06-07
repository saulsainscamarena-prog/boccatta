# Proposal: User Identity and Auth Audit

Spec ID: `identity-auth`
Capability: `User Identity, Firebase Auth, Roles, PIN Flows, Access Control`
Strict TDD: `no`

## Problema

- Se requiere auditar y validar el flujo de identidad, autenticacion, roles, autorizacion de empleados, flujos de PIN/admin, estado de sesion y control de acceso auditable en Bocatta POS.

## Alcance

- Auditar `AuthorizationManager`, `AuthViewModelV2`, y `SessionViewModel`.
- Revisar flujos de PIN (Admin y Empleados).
- Revisar roles y permisos (Superusuario y granulares a traves de `PermisoEmpleado`).
- Revisar limitacion de tasa de intentos de PIN (Rate Limit).
- Revisar bitacora de auditoria.

## Fuera de Alcance

- Modificar la logica de autenticacion de Firebase.
- Refactorizar las pantallas de Login o Admin.

## Archivos Afectados Esperados

- Ninguno (solo auditoria y documentacion).

## Riesgos

- Ventas: N/A
- Inventario: N/A
- Offline/sync: N/A
- Caja/pagos: N/A
- UI: N/A
- Seguridad: N/A

## Research Externo

- Web requerida: no
- Pregunta investigada: N/A
- Fuentes primarias: N/A
- Decision tomada: N/A
- Suposicion sensible a version: N/A

## Estrategia de Rollback

- N/A (no hay cambios de codigo).

## Criterios de Exito

- [x] Documentar el flujo actual de autorizacion y sesion.
- [x] Crear research, proposal, y plan.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
