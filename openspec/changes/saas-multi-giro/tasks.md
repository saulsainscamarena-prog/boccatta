# Tareas: SaaS Multi-Giro (Fase 1 y Fase 2)

## Fase 1: Fundaciones Multi-Tenant
- [x] Actualizar `ModelsV2.kt` añadiendo `tenantId` y `businessType` a los Data Classes principales (`SalesInventoryProductV2`, `ItemCarritoV2`, `VentaV2`, `ClienteV2`, `Gasto`, etc.).
- [x] Actualizar entidades de Room (`OfflineDatabase.kt` / `RoomEntities.kt` o similar) para reflejar `tenantId`.
- [x] Implementar la clase `TenantSessionManager` o adaptar el `SessionContext` para portar el `tenantId` en runtime.
- [x] Adaptar llamadas `db.collection(...)` en los repositorios principales (`FirebaseSalesRepositoryV2`, `FirebaseInventoryRepository`, etc.) para incluir `.whereEqualTo("tenantId", currentTenantId)`.
- [x] Compilar y ejecutar pruebas de regresión en modo local.
- [x] Git commit inicial.

## Fase 2: Motor Multi-Giro y UI Dinámica
- [x] Crear el `BusinessLogicProvider` (BusinessFeatures).
- [x] Ocultar la pantalla de "Recetas" e "Insumos" en el AdminScreen si el giro no requiere recetas (Retail, Servicios).
- [x] Refactorizar la etiqueta de los botones (ej. "Menú" vs "Productos") según el giro.
- [x] Extender la clase `SalesInventoryProductV2` para soportar `tipoProducto`, `requiresStock`, `hasVariants` y `barcode`.
- [x] Compilar y verificar UI dinámicamente según el giro en TenantSessionManager.
- [x] Git commit de la Fase 2.

## Fase 3: Onboarding y Autogestión
- [x] Construir UI de Wizard de Registro (`OnboardingScreen`) con pasos: Registro de Tenant -> Elección de Giro -> Datos de Dueño.
- [x] Crear el Repositorio de Onboarding (`TenantOnboardingRepository`) para interactuar con Firestore.
- [x] Crear archivos JSON en `assets/templates/` para los catálogos por defecto (ej. `template_restaurante.json`, `template_retail.json`).
- [x] Modificar `DataSeederV2` para que lea y construya el inventario inicial de la BD local basándose en la plantilla elegida.
- [x] Crear modelo de `PlanEntitlement` y asociarlo a la creación del Tenant.
- [x] Git commit de la Fase 3.
