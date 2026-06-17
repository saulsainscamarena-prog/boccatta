# Research: Estabilizacion integral de mostrador

Spec ID: `BCT-F15-MOSTRADOR-STABLE`
Capability: `counter_operation_stability`
Fecha: `2026-06-12`

## Brief Humano

El objetivo es llevar Bocatta POS desde un estado funcional pero no confiable
para operacion diaria hasta una version instalable, durable offline, auditable
y verificable en AVD. Mostrador real tiene prioridad sobre Play Store y sobre
features nuevas.

Restricciones:

- La migracion multimodulo pertenece a otro agente.
- No modificar Gradle, catalogo de versiones, navegacion o DI de la migracion
  sin coordinacion.
- No perder ventas cuando la red aparenta estar disponible pero Firestore falla.
- Mantener deducciones de inventario, folios e idempotencia.
- Los cambios se implementaran en fases pequenas con strict TDD.

## Web Research Gate

- Research question: No aplica; los defectos estan definidos por el codigo y
  la evidencia AVD local.
- Source category from `openspec/source-catalog.yaml`: local_first.
- Why local context is insufficient: No es insuficiente.
- Expected decision: Definir ownership, orden y gates de implementacion.
- Stop condition: Owners y contratos locales identificados.
- Budget: none.

Las skills Android fueron verificadas el 2026-06-08, menos de 30 dias antes de
esta spec. No se requiere actualizar decisiones de API ni dependencias.

## Contexto Local Leido

- Constitution: `openspec/constitution.md`.
- AGENTS: `AGENTS.md`.
- Skill registry: `openspec/skill-registry.yaml`.
- Source catalog: `openspec/source-catalog.yaml`.
- Skills: arquitectura, conectividad, Room/SQLite, WorkManager, pruebas y
  estilos Compose.
- Archivos inspeccionados:
  - `CheckoutUseCase.kt`
  - `FirebaseSalesRepositoryV2.kt`
  - `OfflineManager.kt`
  - `OfflineDatabase.kt`
  - `OfflineStorage.kt`
  - `SyncWorker.kt`
  - `SyncScheduler.kt`
  - `SalesViewModelV2.kt`
  - `SalesScreen.kt`
  - `DialogosVentas.kt`
  - `AdminScreen.kt`
  - `InventoryScreen.kt`

## Fuentes Externas Consultadas

No se consultaron fuentes externas.

## Alternativas

### Opcion A: Persistencia local primero para todas las ventas

- Ventajas: maxima durabilidad y un solo origen local.
- Costos: cambia el contrato online completo, folios, tickets y deducciones.
- Riesgos: doble descuento local/remoto y migracion de datos mas amplia.

### Opcion B: Remoto primero con fallback durable clasificado

- Ventajas: conserva el flujo online actual y corrige el hueco operativo.
- Costos: requiere clasificar errores y reconciliar respuestas ambiguas.
- Riesgos: un commit remoto con respuesta perdida puede generar ticket local
  distinto hasta la reconciliacion.

## Decision Recomendada

Aplicar Opcion B en esta fase. Usar el mismo `forcedVentaId`, guardar localmente
solo ante fallos transitorios o ambiguos y reconciliar el ticket remoto cuando
el worker detecte que el documento ya existe. No usar fallback ante stock
insuficiente, permisos, autenticacion o datos invalidos.

## Suposiciones Externas

Ninguna.

## Tradeoffs

- Se prioriza una correccion compatible sobre una reescritura offline-first.
- Los errores transitorios seguiran reintentandose con backoff; solo los
  permanentes pasan a intervencion administrativa.
- La UX de administracion se atiende despues de garantizar durabilidad.

## Preguntas Cerradas

- Owner de checkout: `CheckoutUseCase`.
- Owner de persistencia local: `OfflineManager` + `OfflineDatabase`.
- Owner de reintentos: `SyncWorker` + `SyncScheduler`.
- Owner de estado de mostrador: `SalesViewModelV2`.
- Owner de navegacion administrativa: `AdminScreen`.
