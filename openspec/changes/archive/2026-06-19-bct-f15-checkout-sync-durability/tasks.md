# Tasks: Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`

## Reglas

- Cada tarea debe ser pequena, auditable y reversible.
- No mezclar refactor amplio con correccion funcional.
- Marcar verificacion real por tarea o explicar por que no aplica.

## Tareas

- [x] 1. Confirmar owner actual del comportamiento.
  - Archivos: checkout, sync, VM, DI.
  - Verificacion: inspeccion estatica.

- [x] 2. Ajustar contratos/modelos si aplica.
  - Archivos: `SalesRepository`, `CheckoutUseCase`.
  - Verificacion: unit tests. `ResultadoVenta.alreadyExisted` agregado en commit a3525cff.

- [x] 3. Implementar logica fuera de Composables.
  - Archivos: use cases/repositorios/sync.
  - Verificacion: unit tests. CheckoutUseCase con clasificador de fallos, FirebaseSalesRepositoryV2 con escritura idempotente, SyncWorker con politica KEEP.

- [x] 4. Conectar estado/eventos en ViewModel.
  - Archivos: `SalesViewModelV2`.
  - Verificacion: compile/tests. Flujo de eventos post-venta.

- [x] 5. Actualizar UI Compose.
  - Archivos: `SalesScreen`, `SalesScreenSections`.
  - Verificacion: compile. Scanner fix, logout removido de TopBar, padding grilla.

- [x] 6. Cubrir pruebas criticas.
  - Archivos: `app/src/test`.
  - Verificacion: `testDebugUnitTest`. CheckoutUseCaseDurabilityTest + SalesScreenAjustes.

- [x] 7. Ejecutar verificacion final Windows.
  - Comando: `.\gradlew.bat compileDebugKotlin; .\gradlew.bat testDebugUnitTest; .\gradlew.bat assembleDebug; git diff --check`
  - Resultado: BUILD SUCCESSFUL (compile 26s, tests 36s). Sin errores de diff.

## Checklist Critico

- [ ] Checkout protegido contra doble toque si toca ventas/pagos.
- [ ] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [ ] Deduccion de inventario validada online/offline si aplica.
- [ ] Errores visibles, sin fallos silenciosos.
- [ ] No se agregaron dependencias duplicadas.
- [ ] No se tocaron archivos fuera del alcance.
