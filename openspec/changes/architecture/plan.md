# Plan de Acción: Auditoría y Refactor de Arquitectura

## Identificación del Punto de Entrada
El punto principal de deuda técnica radica en los ViewModels principales, como `SalesViewModelV2`, interactuando con la base de datos de manera directa. El almacenamiento de la capa offline (`OfflineDatabase`) funciona pero carece de la validación de tiempo de compilación.

## Fases del Plan

### Fase 1: Desacoplamiento de Firestore de ViewModels
1. **Actualizar Repositorios:**
   - Añadir métodos en `IProductRepository` (y su implementación) que devuelvan `Flow<List<SalesInventoryProductV2>>` escuchando la colección `PRODUCTOS`.
   - Añadir métodos en `IInventoryRepository` para exponer alertas de stock en tiempo real (`Flow<Map<String, Double>>`).
2. **Refactor de `SalesViewModelV2`:**
   - Eliminar `menuListener`, `stockListener` y las importaciones directas de Firestore.
   - Consumir los `Flow` provenientes de los repositorios usando `collectAsStateWithLifecycle()` o `stateIn()`.
3. **Manejo de Clientes:**
   - Mover la lógica de `buscarCliente` y `registrarClienteNuevo` a un `CustomerRepository` o Caso de Uso, eliminando las consultas `.whereEqualTo` del ViewModel.

### Fase 2: Modernización del Storage Offline (Opcional pero Recomendado)
1. **Diseño de Room Entities:** Crear entidades para `VentaOfflineEntity`, `OperacionOfflineEntity`, `TurnoContingenciaEntity` mapeando la estructura existente.
2. **Implementación de DAOs:** Construir DAOs que imiten las firmas actuales de `OfflineDatabase`, usando `@Transaction` donde se requiere atomicidad.
3. **Estrategia de Migración:** Mantener `OfflineDatabase.kt` para dispositivos en producción, pero preparar la base de Room con un fallback destructivo para datos en caché.
   - *Nota de la Constitución:* Todo cambio de stock/ventas offline debe validarse rigurosamente.

### Fase 3: Verificación
- Ejecutar suite de pruebas actual:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "*Koin*" --tests "*Offline*"
  .\gradlew.bat compileDebugKotlin
  ```
- Pruebas manuales simulando caídas de red para comprobar que la lógica local (ahora aislada) interactúa correctamente con los nuevos Flows y repoblando las tablas.

**Nota:** Este documento se genera solo para auditoría según los requerimientos solicitados, sin modificar código todavía.
