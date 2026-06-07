# Research: Audit Background Work and Offline Sync

Spec ID: `sync-worker`
Capability: `Offline Sync / Background Work`
Fecha: `2026-06-06`

## Brief Humano

Auditar el trabajo en segundo plano y los mecanismos de sincronización offline de Bocatta POS. Específicamente, revisar `SyncWorker`, políticas de reintentos, restricciones de foreground/background, y trabajo programado amigable con la batería. La tarea exige solo revisión sin modificación de código.

## Web Research Gate

- Research question: N/A
- Source category from `openspec/source-catalog.yaml`: N/A
- Why local context is insufficient: N/A - Local context is sufficient for an audit.
- Expected decision: N/A
- Stop condition: N/A
- Budget: none

Local context is sufficient because the task is an audit of the current internal implementation of Bocatta POS background work mechanisms.

## Contexto Local Leido

- Constitution: `openspec/constitution.md`
- AGENTS: `AGENTS.md`
- Skill registry: N/A
- Source catalog: N/A
- Archivos inspeccionados:
  - `app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt`
  - `app/src/main/java/com/bocatta/pos/data/sync/SyncScheduler.kt`
  - `app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt`
  - `app/src/main/java/com/bocatta/pos/data/queue/SQLiteStockAdjustmentQueue.kt`

## Hallazgos de Auditoría (Alternativas/Notas de Implementación actual)

- **WorkManager vs Foreground Services**: El proyecto usa correctamente `WorkManager` (tanto `PeriodicWorkRequestBuilder` como `OneTimeWorkRequestBuilder`), respetando las restricciones modernas de Android (12+) para background execution sin requerir servicios foreground abusivos.
- **Battery-Safe**: Los requests de sync tienen Constraints de `NetworkType.CONNECTED`, asegurando que no se gaste batería intentando red cuando no hay. Los retries usan `BackoffPolicy.EXPONENTIAL`. El periódico corre cada 15 mins.
- **Retry Policy**: `SyncWorker` extrae un `max_retries` de `inputData` (default 3) y usa `runAttemptCount` para reportar un error a la cola si se excede. Además, maneja `intentos` individuales por `VentaOffline` y `OperacionOffline` antes de marcarlas como fallidas críticas.
- **Idempotencia**: Durante el sync de ventas en Firestore, el worker checa si el documento de venta ya existe usando transacciones de Firestore, saltando la deducción de inventario para no descontar doble si un sync falló a medio camino previamente.
- **Manejo de Stock**: Usa `SQLiteStockAdjustmentQueue` local para encolar ajustes. Para recuperar crashes, tiene mecanismo `resetStaleSyncing(STALE_SYNCING_MAX_AGE_MS)` que previene "locks" infinitos en estado "syncing".

## Decision Recomendada

- La implementación es robusta y cumple con las buenas prácticas de Android y los lineamientos de `AGENTS.md`. No se requieren modificaciones en este momento.

## Suposiciones Externas

- Ninguna

## Tradeoffs

- El periodo de 15 minutos en WorkManager no es exacto debido al modo Doze y a las optimizaciones del SO Android, pero está bien para offline sync.

## Preguntas Que Deben Aclararse Antes de Proponer

- Ninguna.
