# Tasks: Optimizacion Checkout y Mosaicos

Spec ID: `optimizacion-checkout-y-mosaicos`

## Reglas
- Cada tarea debe ser pequeña, auditable y reversible.
- No mezclar refactor amplio con corrección funcional.
- Marcar verificación real por tarea o explicar por qué no aplica.

## Tareas

- [ ] Tarea 1: Modificar FirebaseSalesRepositoryV2 y SyncWorker (Optimización de checkout)
  - Archivos: [FirebaseSalesRepositoryV2.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/repository/FirebaseSalesRepositoryV2.kt), [SyncWorker.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt), [AppModules.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/di/AppModules.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

- [ ] Tarea 2: Actualizar InventoryRepository para compras locales
  - Archivos: [InventoryRepository.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/repository/InventoryRepository.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

- [ ] Tarea 3: Crear AjustesAlmacenScreen
  - Archivos: [AjustesAlmacenScreen.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/presentation/ui/screens/inventario/AjustesAlmacenScreen.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

- [ ] Tarea 4: Modificar DialogProductoV2 (Toppings dinámicos y advertencia de precios)
  - Archivos: [DialogProductoV2.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/DialogProductoV2.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

- [ ] Tarea 5: Agregar lógica de v2_employees en AdminViewModel
  - Archivos: [AdminViewModel.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/presentation/viewmodel/AdminViewModel.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

- [ ] Tarea 6: Rediseñar DialogEmpleado con sub-diálogos (Progressive Disclosure)
  - Archivos: [DialogEmpleado.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/DialogEmpleado.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

- [ ] Tarea 7: Rediseñar AdminScreen con Mosaico 3x2 y sub-pantallas
  - Archivos: [AdminScreen.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/AdminScreen.kt)
  - Verificación: `.\gradlew.bat compileDebugKotlin`

## Checklist Crítico
- [ ] Checkout protegido contra doble toque si toca ventas/pagos.
- [ ] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [ ] Deducción de inventario validada online/offline si aplica.
- [ ] Errores visibles, sin fallos silenciosos.
- [ ] No se agregaron dependencias duplicadas.
- [ ] No se tocaron archivos fuera del alcance.
