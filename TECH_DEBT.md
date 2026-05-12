# Technical debt

## Room / KSP (RESOLVED)
- KSP updated to `2.2.10-2.0.2`. Plugin + Room runtime re-enabled.
- Room files restored from `room_disabled_backup/`. KSP2 `2.3.7` tested but has `unexpected jvm signature V` with current entities — using native SQLite for now.
- **Future‑proofing**: KSP 2.3.0+ is standalone (not tied to Kotlin version).

## Legacy model consolidation (RESOLVED)
- `ProductoV2`/`SalesProductV2` → `SalesInventoryProductV2` consolidated.
- `ProductMappers.kt` tagged `@Deprecated` — removal scheduled for v2.2.
- **Next step (v2.2)**: Migrate ViewModel Firestore listeners to `IProductRepository.getAllProducts(): Flow<InventoryProductV2>`.

## Test coverage (2026-05-11)
- **218 unit tests, 0 failures** (up from 187).
- **New tests**: 14 queue stress, 6 two-phase commit, 11 DynamicFormEngine, 13 Compose instrumented.

## Theming & accessibility (2026-05-11)
- **5 secondary screens** migrated from hardcoded colors to `MaterialTheme.colorScheme` tokens:
  AdminScreen, InventoryScreen, ReportScreen, GastosScreen, ClientesScreen.
- **3 cards** with hardcoded alpha replaced with `primaryContainer`/`surfaceVariant`.
- **contentDescription** added to interactive icons in SalesScreen, ClientesScreen, InventoryScreen.

## Dynamic multi‑giro form engine (2026-05-11)
- `DynamicFormEngine` with 3 schemas: FOOD (6 fields), RETAIL (5), SERVICE (5).
- `DynamicProductForm` + `GiroSelector` composables ready.
- **Pending**: Load real `product_definitions` from Firestore to validate non‑food giro rendering on physical devices.

---
