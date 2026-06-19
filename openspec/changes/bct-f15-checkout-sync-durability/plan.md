# Plan Tecnico: Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Owners actuales de checkout/sync/cancelacion.

## Punto de Entrada Real

- UI: `SalesScreen` dispara cancelacion/cobro.
- ViewModel: `SalesViewModelV2`.
- Dominio/use case: `CheckoutUseCase`, `RegistrarCancelacionUseCase`.
- Repositorio/data: `FirebaseSalesRepositoryV2`, `OfflineManager`.
- DI: `AppModules`, `SalesDependencies`.
- Tests: `app/src/test`.

## Archivos Probables

- `SalesViewModelV2`, `SalesScreen`, `CheckoutUseCase`, `FirebaseSalesRepositoryV2`, `SyncWorker`, `SyncScheduler`.
- Tests de checkout y cancelacion.

## Estrategia

1. Wiring de cancelacion y SDD/notas.
2. Checkout durable y limpieza post-venta.
3. Idempotencia online/sync.
4. Tests y verificacion.

## Contratos a Revisar

- Modelos: `ResultadoVenta`, `CheckoutResult`.
- Interfaces: `SalesRepository`.
- Koin: principal y data module.
- Firestore/Room: sin schema nuevo.
- WorkManager/sync: politica de unique work.

## Impacto Offline

- Venta online: preferida si Firestore responde.
- Venta offline: fallback para transitorios.
- Reconexion: no duplicar venta existente.
- Deduccion de inventario: sin cambio de formula.
- Idempotencia/reintentos: venta ID estable.

## Impacto UI

- Movil/tablet: sin rediseno.
- Estados de carga/error: mensajes estables.
- Eventos consumibles: exito cierra modal y limpia carrito.
- Accesibilidad: sin alcance visual.

## Plan de Pruebas

- Unit: checkout/cancelacion/classifier.
- Instrumented: no obligatorio en esta fase si no hay AVD estable.
- Compose UI: no obligatorio.
- Maestro/manual: AVD si queda disponible.
- Gradle: compile, unit, assemble, diff check.

## Criterio para No Continuar

- Gradle/daemon de otra sesion activo durante verificacion.
- Cambio requiere modificar Gradle o schema persistente.
