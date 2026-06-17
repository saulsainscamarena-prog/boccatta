# Implementation Tasks: Multimodule Migration

Spec ID: `multimodule-migration`

## Delivery Strategy

`exception-ok`

## Phase 1: Build logic
- [x] Task 1.1: Create `build-logic` directory and its `settings.gradle.kts` to register it as an included build.
- [x] Task 1.2: Implement Convention Plugins (`bocatta.android.application`, `bocatta.android.library`, `bocatta.android.feature`, `bocatta.android.compose`) within `build-logic`, pulling dependencies from `libs.versions.toml`.
- [x] Task 1.3: Update the root `build.gradle.kts` and main `settings.gradle.kts` to include `build-logic` plugins. Verify the project configures and compiles successfully.

## Phase 2: Core modules extraction
- [x] Task 2.1: Extract `:core:model`. Move domain entities, generic data classes, and enums from `:app` to `:core:model`. Update `:app` dependencies.
- [x] Task 2.2: Extract `:core:database`. Move Room entities, DAOs, and Database definition. Create its Koin module (`databaseModule`). Update `:app` dependencies.
- [x] Task 2.3: Extract `:core:network`. Move Retrofit interfaces, interceptors, and DTOs. Create its Koin module (`networkModule`). Update `:app` dependencies.
- [x] Task 2.4: Extract `:core:ui`. Move common Compose components, `theme`, typography, color definitions, and shared resources. Fix UI imports in `:app`.

## Phase 3: Feature modules extraction
- [x] Task 3.1: Extract `:feature:auth`. Move authentication screens, viewmodels, and feature Koin module. Create `AuthNavigation.kt` to expose routes to `:app`.
- [x] Task 3.2: Extract `:feature:ventas`. Move sales screens, viewmodels, and feature Koin module. Create `VentasNavigation.kt`. Ensure it only depends on `:core` modules.
- [x] Task 3.3: Extract `:feature:inventario`. Move inventory screens, viewmodels, and feature Koin module. Create `InventarioNavigation.kt`.
- [x] Task 3.4: Extract `:feature:admin`. Move admin screens, viewmodels, and feature Koin module. Create `AdminNavigation.kt`.

## Phase 4: App module cleanup
- [x] Task 4.1: Refactor `AppNavGraph` in `:app`. It must consume the navigation extension functions from the feature modules rather than direct composables.
- [x] Task 4.2: Update the custom `Application` class in `:app` to aggregate all Koin modules (`databaseModule`, `networkModule`, `authModule`, `ventasModule`, `inventarioModule`, etc.).
- [x] Task 4.3: Verify `:app` `build.gradle.kts`. Ensure it only depends on the required `:feature` and `:core` modules, removing obsolete direct library dependencies that are now in convention plugins.
- [x] Task 4.4: Full Verification. Run `.\gradlew.bat assembleDebug` and `.\gradlew.bat test` to confirm there are no circular dependencies and the app functions identically.

## Review Workload Forecast

- **Phase 1: Build logic**: Medium (~300 lines). Setting up convention plugins, creating the included build, and updating `settings.gradle.kts`.
- **Phase 2: Core modules extraction**: Large (> 500 lines). Creating new module directories, migrating many foundational classes, refactoring imports, and extracting Koin modules.
- **Phase 3: Feature modules extraction**: Large (> 500 lines). Moving major screens/viewmodels, establishing strict `NavGraphBuilder` boundaries, and fixing all broken imports.
- **Phase 4: App module cleanup**: Small (< 100 lines). Final wiring of the central `AppNavGraph`, updating the DI initialization, and pruning dependencies in `:app`.
