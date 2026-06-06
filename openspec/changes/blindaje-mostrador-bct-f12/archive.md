# Archive: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`
Capability: `mostrador-offline-sync-auth-ticket`
Fecha: `2026-06-06`

## Resultado Final

- Implementacion funcional completada en alcance quirurgico.
- Cierre formal aprobado para BCT-F12 tras compilacion y unit tests.

## Cambios Entregados

- Apertura de turno para vendedor sin list query de empleados.
- Recuperacion de ajustes de stock offline stale en `SyncWorker`.
- Sincronizacion de ajustes offline con cantidad base.
- Ticket WhatsApp con descuento manual.
- Undo del carrito centralizado en `CartManager`.
- Tests unitarios/contrato agregados.
- Pendientes BCT-F12 registrados en `notas.txt`.

## Verificacion Ejecutada

- `git diff --check` del alcance: aprobado.
- `compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `testDebugUnitTest`: `BUILD SUCCESSFUL`.
- AVD/manual: pendiente.

## Decisiones Preservadas

- Mostrador real tiene prioridad sobre Play Store.
- No tocar Gradle ni versiones por trabajo paralelo.
- No redisenar admin/inventario dentro de esta fase.
- No crear arquitectura nueva si el owner existente resuelve el problema.

## Pendientes o Deuda Tecnica

- Ejecutar Gradle cuando no haya procesos concurrentes.
- Spec posterior para administracion real de stock y UX de administrar tienda.
- Spec posterior para limpiar frontera de `CheckoutUseCase`.
- Spec posterior para Room/SQLite, migraciones y `fallbackToDestructiveMigration`.

## Ubicacion de Archivo

Cuando el cambio este cerrado, mover esta carpeta a:

```text
openspec/changes/archive/2026-06-06-bct-f12-blindaje-mostrador/
```
