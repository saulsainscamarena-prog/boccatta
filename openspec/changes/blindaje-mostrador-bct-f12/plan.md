# Plan Tecnico: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Skills locales de arquitectura, background work, offline storage, auth y testing.
- Archivos propietarios del flujo real antes de editar codigo.

## Punto de Entrada Real

- UI: no aplica en esta fase.
- ViewModel: `SalesViewModelV2` delega undo y checkout hacia use cases/manager.
- Dominio/use case: `AuthorizationManager`, `CheckoutUseCase`, `CartManager`, `GenerarTicketWhatsAppUseCase`.
- Repositorio/data: `InventoryRepositoryImpl`, `SyncWorker`.
- DI: no se modifica.
- Tests: unit/contract tests existentes en `app/src/test/java`.

## Archivos Probables

- `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/InventoryRepositoryImpl.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCase.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/CheckoutUseCase.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/CartManager.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SalesViewModelV2.kt`
- `app/src/test/java/com/bocatta/pos/domain/usecase/AuthorizationManagerTest.kt`
- `app/src/test/java/com/bocatta/pos/security/PinAuthorizationRegressionTest.kt`
- `app/src/test/java/com/bocatta/pos/security/OfflineOperationsContractTest.kt`
- `app/src/test/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCaseTest.kt`
- `notas.txt`

## Estrategia

1. Resolver apertura de turno para vendedor en `AuthorizationManager` sin depender de list query.
2. Recuperar estados stale en `SyncWorker` antes de procesar pendientes.
3. Normalizar cantidades de stock offline a unidad base en repositorio remoto.
4. Alinear ticket WhatsApp con descuento manual.
5. Centralizar undo en `CartManager`.
6. Agregar tests de contrato/unitarios.
7. Saneamiento puntual de whitespace en archivos tocados.
8. Verificar con `git diff --check` del alcance y Gradle cuando no haya procesos concurrentes.

## Contratos a Revisar

- Modelos: sin cambios.
- Interfaces: `IStockAdjustmentQueue`.
- Koin: sin cambios.
- Firestore/Room: sin schema nuevo.
- WorkManager/sync: `SyncWorker.doWork`.

## Impacto Offline

- Venta online: sin cambios en persistencia principal.
- Venta offline: no se altera guardado de venta.
- Reconexion: stock adjustment queue puede recuperar estados antiguos.
- Deduccion de inventario: cantidades remotas salen en base.
- Idempotencia/reintentos: no se marca completado antes de sync remoto.

## Impacto UI

- Movil: sin cambios visuales.
- Tablet: sin cambios visuales.
- Estados de carga/error: sin cambios.
- Eventos consumibles: undo queda centralizado.
- Accesibilidad: no aplica.

## Plan de Pruebas

- Unit:
  - `AuthorizationManagerTest`
  - `GenerarTicketWhatsAppUseCaseTest`
- Contract:
  - `PinAuthorizationRegressionTest`
  - `OfflineOperationsContractTest`
- Instrumented:
  - Pendiente, no requerido para esta fase quirurgica salvo regresion.
- Compose UI:
  - Pendiente, no hubo UI.
- Maestro/manual:
  - Abrir turno vendedor, cobrar con descuento manual y probar undo en carrito.
- Gradle:
  - `.\gradlew.bat compileDebugKotlin --no-daemon --no-parallel --max-workers=1`
  - `.\gradlew.bat testDebugUnitTest --no-daemon --no-parallel --max-workers=1`

## Criterio para No Continuar

- Hay procesos Java/Gradle activos de otra sesion y correr Gradle puede pisarlos.
- Aparece fallo de compilacion en owner critico que requiere cambio amplio.
- Se detecta que una regla Firestore exige cambio coordinado de backend.
- El usuario pide pausar para revisar UX/admin/stock antes de tocar mas codigo.
