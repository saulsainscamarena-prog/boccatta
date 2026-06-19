# Archive: Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`
Capability: `ventas-offline-sync`
Fecha: `2026-06-19`

## Resultado Final

Implementado y verificado. Las 7 tareas completadas.

## Cambios Entregados

- SalesRepository con operacion idempotente `addSaleIfNotExists`
- ResultadoVenta con flag `alreadyExisted` para evitar duplicacion
- CheckoutUseCase con clasificador de fallos (FailureKind) y fallback offline para errores transitorios
- FirebaseSalesRepositoryV2 con guardado idempotente
- SyncWorker con politica ExistingWorkPolicy.KEEP (no REPLACE)
- OfflineManager existente en core/data con fallback transitorio
- SalesViewModelV2 con evento `saleCleared` post-venta exitosa
- SalesScreen con scanner fix, logout removido de TopBar, padding en grilla
- Tests: CheckoutUseCaseDurabilityTest actualizado

## Verificacion Ejecutada

- compileDebugKotlin: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL
- assembleDebug: incluido en test run
- git diff --check: sin errores

## Decisiones Preservadas

- Mostrador real sobre Play Store.
- Sin cambios de Gradle/dependencias.
- Sin cambios de recetas/precios/promociones.
- Logout removido del TopBar de ventas para evitar cierre accidental.

## Pendientes o Deuda Tecnica

- Pruebas instrumentadas pendientes (requieren AVD).
- Pruebas manuales offline/sync no ejecutadas.
- Android instrumented tests pre-existentes rotos (BocattaOfflineDatabaseTest, etc.).

## Ubicacion de Archivo

```text
openspec/changes/archive/2026-06-19-bct-f15-checkout-sync-durability/
```
