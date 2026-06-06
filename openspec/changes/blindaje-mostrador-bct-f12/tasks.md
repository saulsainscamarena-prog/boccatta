# Tasks: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`

## Reglas

- Cada tarea debe ser pequena, auditable y reversible.
- No mezclar refactor amplio con correccion funcional.
- Marcar verificacion real por tarea o explicar por que no aplica.

## Tareas

- [x] 1. Confirmar owner actual del comportamiento.
  - Archivos: `AuthorizationManager`, `SyncWorker`, `InventoryRepositoryImpl`, `CheckoutUseCase`, `CartManager`, `SalesViewModelV2`.
  - Verificacion: lectura multi-archivo y skills locales.

- [x] 2. Ajustar contratos/modelos si aplica.
  - Archivos: `GenerarTicketWhatsAppUseCase.kt`.
  - Verificacion: parametro default mantiene compatibilidad de llamadas existentes.

- [x] 3. Implementar logica fuera de Composables.
  - Archivos: `AuthorizationManager.kt`, `SyncWorker.kt`, `InventoryRepositoryImpl.kt`, `CheckoutUseCase.kt`, `CartManager.kt`.
  - Verificacion: sin cambios en UI Compose.

- [x] 4. Conectar estado/eventos en ViewModel.
  - Archivos: `SalesViewModelV2.kt`.
  - Verificacion: VM delega undo a `CartManager`.

- [x] 5. Actualizar UI Compose.
  - Archivos: no aplica.
  - Verificacion: no hubo UI nueva.

- [x] 6. Cubrir pruebas criticas.
  - Archivos: `AuthorizationManagerTest.kt`, `PinAuthorizationRegressionTest.kt`, `OfflineOperationsContractTest.kt`, `GenerarTicketWhatsAppUseCaseTest.kt`.
  - Verificacion: tests agregados; ejecucion Gradle pendiente por concurrencia.

- [x] 7. Ejecutar verificacion final Windows.
  - Comando: `.\gradlew.bat compileDebugKotlin --no-daemon --no-parallel --max-workers=1; .\gradlew.bat testDebugUnitTest --no-daemon --no-parallel --max-workers=1`
  - Resultado: `BUILD SUCCESSFUL` en ambos comandos.

## Checklist Critico

- [x] Checkout protegido contra doble toque si toca ventas/pagos.
- [x] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [x] Deduccion de inventario validada online/offline si aplica.
- [x] Errores visibles, sin fallos silenciosos.
- [x] No se agregaron dependencias duplicadas.
- [x] No se tocaron archivos Gradle/version catalog en esta fase.
- [x] Gradle final ejecutado sin conflicto de procesos.
