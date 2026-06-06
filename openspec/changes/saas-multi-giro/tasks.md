# Tareas: SaaS Multi-Giro (Fase 1: Fundaciones)

## Fase 1: Fundaciones Multi-Tenant
- [x] Actualizar `ModelsV2.kt` añadiendo `tenantId` y `businessType` a los Data Classes principales (`SalesInventoryProductV2`, `ItemCarritoV2`, `VentaV2`, `ClienteV2`, `Gasto`, etc.).
- [x] Actualizar entidades de Room (`OfflineDatabase.kt` / `RoomEntities.kt` o similar) para reflejar `tenantId`.
- [x] Implementar la clase `TenantSessionManager` o adaptar el `SessionContext` para portar el `tenantId` en runtime.
- [x] Adaptar llamadas `db.collection(...)` en los repositorios principales (`FirebaseSalesRepositoryV2`, `FirebaseInventoryRepository`, etc.) para incluir `.whereEqualTo("tenantId", currentTenantId)`.
- [x] Compilar y ejecutar pruebas de regresión en modo local.
- [x] Git commit inicial.
