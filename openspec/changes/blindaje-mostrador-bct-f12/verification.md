# Verification: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`

## Entorno

- Windows: si.
- JDK: wrapper ejecuto correctamente con daemon de un solo uso.
- Gradle wrapper: `.\gradlew.bat`.
- Emulador/dispositivo: no usado en esta fase.
- Fecha: `2026-06-06`.

## Comandos

```powershell
git diff --check -- app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt app/src/main/java/com/bocatta/pos/data/repository/InventoryRepositoryImpl.kt app/src/main/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCase.kt app/src/main/java/com/bocatta/pos/domain/usecase/CheckoutUseCase.kt app/src/main/java/com/bocatta/pos/domain/usecase/CartManager.kt app/src/main/java/com/bocatta/pos/presentation/viewmodel/SalesViewModelV2.kt app/src/test/java/com/bocatta/pos/domain/usecase/AuthorizationManagerTest.kt app/src/test/java/com/bocatta/pos/security/PinAuthorizationRegressionTest.kt app/src/test/java/com/bocatta/pos/security/OfflineOperationsContractTest.kt app/src/test/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCaseTest.kt notas.txt
.\gradlew.bat compileDebugKotlin --no-daemon --no-parallel --max-workers=1
.\gradlew.bat testDebugUnitTest --no-daemon --no-parallel --max-workers=1
```

## Resultados

- Compilacion: `BUILD SUCCESSFUL` con `compileDebugKotlin --no-daemon --no-parallel --max-workers=1`.
- Unit tests: `BUILD SUCCESSFUL` con `testDebugUnitTest --no-daemon --no-parallel --max-workers=1`.
- Instrumented tests: no ejecutados; no hubo UI ni schema.
- Manual: pendiente.
- `git diff --check` del alcance: aprobado tras corregir whitespace puntual en `SyncWorker.kt`.

## Pruebas Manuales

### Movil

- [ ] Abrir turno como vendedor.
- [ ] Agregar producto, eliminar, usar undo y confirmar que no duplica estado.

### Tablet

- [ ] Cobrar venta con descuento manual y revisar ticket WhatsApp.
- [ ] Confirmar que no hay regresion visual en mostrador.

### Offline

- [ ] Sin internet: generar ajuste/merma o venta con deduccion local.
- [ ] Reconexion: worker recupera ajustes stale.
- [ ] Sync: Firestore recibe cantidad base y movimiento `unit=base`.

## Evidencia

- Logs: Gradle genero reporte de problemas en `build/reports/problems/problems-report.html`; solo warnings de features deprecadas/configuration cache sugerida.
- Capturas: no aplica.
- Observaciones: la verificacion Gradle debe ejecutarse sin pisar procesos del trabajo paralelo.

## Incidencias

- No se uso `--stop` ni se mataron daemons compartidos.
- Full repo `git diff --check` puede fallar por deuda previa fuera del alcance; usar diff del alcance para esta fase.
