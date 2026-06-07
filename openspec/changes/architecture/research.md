# Research: Architecture Audit

## Contexto
El usuario solicitó una auditoría de las decisiones de arquitectura de Android en Bocatta POS, enfocándose en ViewModels, repositorios, casos de uso, límites de dominio/datos, y los esquemas de almacenamiento offline (SQLite/Room).

## Estado Actual de la Arquitectura

### 1. ViewModels y Manejo de Estado
- **Ejemplo Analizado:** `SalesViewModelV2.kt` (1100+ líneas).
- **Observaciones:**
  - Utiliza `StateFlow` y `mutableStateOf` adecuadamente para mantener el estado de la UI (cumple con las reglas de Compose).
  - Delega lógica compleja a Use Cases (`CartManager`, `PromocionesEngine`, `CatalogoOperativoUseCase`).
  - **Deuda Técnica / Fuga de Límites:** El ViewModel todavía mantiene referencias directas a `FirebaseFirestoreProvider.db` y gestiona `SnapshotListeners` (ej. `escucharMenu()`, `escucharStock()`, `buscarCliente()`). Esto rompe el límite entre la capa de presentación y la capa de datos. Idealmente, los ViewModels deberían consumir `Flows` emitidos por Repositorios o Use Cases.

### 2. Repositorios y Casos de Uso
- **Casos de Uso (Domain):** Existen múltiples casos de uso bien definidos (ej. `SalesFlowUseCase`, `CheckoutUseCase`, `GenerarTicketWhatsAppUseCase`) que encapsulan reglas de negocio.
- **Repositorios (Data):** Abstraen la lógica de sincronización (`OperationalCatalogSyncRepository`, `InventoryDeductions`). Existe un buen intento de separar interfaces (`IProductRepository`, `IInventoryRepository`) de sus implementaciones.

### 3. Almacenamiento Offline (SQLite)
- **Ejemplo Analizado:** `OfflineDatabase.kt` (1000+ líneas).
- **Observaciones:**
  - Utiliza `SQLiteOpenHelper` con sentencias SQL crudas (raw SQL) en lugar de Android Room.
  - El esquema persiste: `ventas_pendientes`, `folios_offline`, `operaciones_pendientes`, `held_orders`, `registro_jornadas`, `turnos_contingencia`, `insumos_v2`, `productos_v2`, etc.
  - Implementa transacciones atómicas de forma explícita (`guardarVentaYDescontarStockReservandoFolio`), protegiendo el cobro y la deducción de inventario, lo cual es crítico según `constitution.md`.
  - Existe un buen manejo de colas offline (`estado`, `intentos`, `ultimoIntento`) para gestionar la sincronización posterior.

### 4. Límites Dominio/Datos
- Algunos modelos parecen compartirse entre la capa de red (Firestore), la base de datos local y la UI (ej. `SalesInventoryProductV2`, `VentaOffline`).
- Falta un mapeo estricto (Mappers) entre DTOs de red, Entidades Locales (SQLite) y Modelos de Dominio puros, aunque para una app de este tamaño, el patrón actual reduce el boilerplate.

## Riesgos y Problemas Identificados
1. **Acoplamiento UI-Datos:** Consultas Firestore directamente en ViewModels dificultan las pruebas unitarias aisladas y las vistas previas de Compose.
2. **Mantenibilidad de SQLite:** Mantener esquemas largos y migraciones en strings (raw SQL) en `OfflineDatabase.kt` es propenso a errores en tiempo de ejecución. Room ofrecería validación en tiempo de compilación.
3. **Tamaño de ViewModels:** `SalesViewModelV2` tiene muchas responsabilidades acumuladas (carrito, clientes, métodos de pago, split, Firestore listeners).
