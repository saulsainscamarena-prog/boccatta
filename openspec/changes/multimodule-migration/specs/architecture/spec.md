# Spec: Architecture (Multimodule Migration)

Spec ID: `multimodule-migration`
Capability: `architecture`

## 1. Functional Requirements

This is a pure structural and architectural migration. There are **NO** functional requirement changes to the application's behavior. The application MUST behave exactly as it did before the migration.
* **Ventas**: Must continue processing sales as usual.
* **Inventario**: Must continue tracking stock and offline sync.
* **Auth**: Employee login and PIN flows must remain identical.
* **UI**: All screens, navigation patterns, and styles must be preserved.

## 2. Structural Requirements & Constraints

The monolithic `:app` module MUST be divided into a layered modular architecture following modern Android development practices (similar to *Now in Android*).

### 2.1. Module Categories
* **`:app`**: Glue module. MUST ONLY contain application-level configuration, root Koin dependency injection setup, and global navigation (`AppNavGraph`). MUST NOT contain business logic.
* **`:core:*`**: Foundation modules providing shared utilities, UI components, data sources, and models.
    * MUST NOT depend on any `:feature:*` modules.
    * Examples: `:core:model`, `:core:database`, `:core:network`, `:core:ui`.
* **`:feature:*`**: Modules containing business logic and UI for specific functional areas.
    * MUST depend only on necessary `:core:*` modules.
    * MUST NOT depend on other `:feature:*` modules directly.
    * Examples: `:feature:auth`, `:feature:admin`, `:feature:ventas`, `:feature:inventario`.

### 2.2. Build System
* **Convention Plugins**: MUST use Gradle Convention Plugins located in a `build-logic` directory to centralize and share build configuration across all modules (e.g., `bocatta.android.application`, `bocatta.android.library`, `bocatta.android.feature`, `bocatta.android.compose`).
* **Version Catalogs**: MUST continue using `libs.versions.toml` for dependency management. Convention plugins MUST consume dependencies from the catalog.
* **Configuration Cache**: MUST maintain compatibility with Gradle Configuration Cache.

### 2.3. Dependency Management Constraints
* **No Circular Dependencies**: The module graph MUST form a Directed Acyclic Graph (DAG).
* **Strict Encapsulation**: Modules MUST use `internal` visibility modifiers for classes and functions that should not be exposed to other modules (e.g., specific DAO implementations, local UI state models).
* **Dependency Injection**: Koin MUST be used to wire dependencies across modules. Each module SHOULD define its own Koin module definition, which is then aggregated by the `:app` module.

## 3. Interfaces & Contracts

* **Feature Navigation**: Features MUST expose their navigation graphs and route objects publicly so the `:app` module can assemble them into the `AppNavGraph`. Features MUST NOT directly instantiate each other's screens.
* **Data Access**: `core:database` and `core:network` MUST expose clean repository interfaces or domain models to the feature modules. Concrete implementation details (like Retrofit services or Room DAOs) SHOULD remain internal where possible.

## 4. Edge Cases and Mitigations

* **Offline Sync (`SyncWorker`)**: Worker registration and execution must be verified. Since the worker might depend on multiple feature databases or repositories, it must be carefully placed (likely in a `core:sync` or similar, or wired correctly via Koin) to avoid circular dependencies.
* **Koin Resolution Errors**: Because dependency resolution happens at runtime in Koin, moving classes to different modules may cause crashes if a Koin module is forgotten in the `:app` module assembly. Mitigation: Provide strict unit tests checking Koin module verification (e.g., `checkModules`).
* **Resource Clashes**: Multiple modules might define the same resource name (e.g., `R.string.app_name`). Mitigation: Keep global resources in `:core:ui` or `:core:designsystem` and prefix feature-specific resources if necessary.

## 5. Security and Privacy

* Internal business rules and data models that were previously globally accessible in `:app` should now be restricted to their respective modules using `internal` visibility.

## 6. Testing Implications

* Since this is a structural change, the primary testing strategy involves verifying that the app compiles successfully (`Build Successful`) and that existing unit/instrumented tests continue to pass without modification to their assertions.
* The test suites must be executed after each phase of the migration.
