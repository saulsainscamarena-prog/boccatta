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

- [ ] 2. Ajustar contratos/modelos si aplica.
  - Archivos: `SalesRepository`, `CheckoutUseCase`.
  - Verificacion: unit tests.

- [ ] 3. Implementar logica fuera de Composables.
  - Archivos: use cases/repositorios/sync.
  - Verificacion: unit tests.

- [ ] 4. Conectar estado/eventos en ViewModel.
  - Archivos: `SalesViewModelV2`.
  - Verificacion: compile/tests.

- [ ] 5. Actualizar UI Compose.
  - Archivos: `SalesScreen`.
  - Verificacion: compile.

- [ ] 6. Cubrir pruebas criticas.
  - Archivos: `app/src/test`.
  - Verificacion: `testDebugUnitTest`.

- [ ] 7. Ejecutar verificacion final Windows.
  - Comando: `.\gradlew.bat compileDebugKotlin; .\gradlew.bat testDebugUnitTest; .\gradlew.bat assembleDebug; git diff --check`
  - Resultado:

## Checklist Critico

- [ ] Checkout protegido contra doble toque si toca ventas/pagos.
- [ ] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [ ] Deduccion de inventario validada online/offline si aplica.
- [ ] Errores visibles, sin fallos silenciosos.
- [ ] No se agregaron dependencias duplicadas.
- [ ] No se tocaron archivos fuera del alcance.
