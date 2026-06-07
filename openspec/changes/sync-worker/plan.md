# Plan Tecnico: Audit Background Work and Offline Sync

Spec ID: `sync-worker`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Archivos propietarios del flujo real antes de editar codigo.

## Punto de Entrada Real

- WorkManager y sus requests en `SyncScheduler.kt`.
- Su worker asignado es `SyncWorker.kt`.
- La orquestación y llamado inicia en `OfflineManager.kt`.
- Todos han sido validados.

## Archivos Probables

- N/A (Solo revisión)

## Estrategia

1. Inspeccionar `SyncWorker` y `SyncScheduler`.
2. Validar manejo de batería (Constraints).
3. Validar reintentos (Exponential Backoff y counters de SQLite y de RunAttempt).
4. Redactar reportes.
5. Avisar al usuario.

## Contratos a Revisar

- Modelos: VentaOffline, StockAdjustmentEntity.
- Interfaces: IStockAdjustmentQueue.
- Koin: N/A.
- Firestore/Room: N/A.
- WorkManager/sync: Constraints, Retry Policies, Exponential Backoff.

## Impacto Offline

- Venta online: N/A
- Venta offline: N/A
- Reconexion: Validado. La reconexión dispara `SyncWorker` si hay request pendiente.
- Deduccion de inventario: Validado.
- Idempotencia/reintentos: Validado. Hay chequeo de `exists()` antes de procesar ventas en Firestore, evitando corrupciones por repetición.

## Impacto UI

- N/A

## Plan de Pruebas

- Unit: N/A
- Instrumented: N/A
- Compose UI: N/A
- Maestro/manual: N/A
- Gradle: N/A

## Criterio para No Continuar

- Modificar el comportamiento actual no es necesario, detener plan de implementación.
