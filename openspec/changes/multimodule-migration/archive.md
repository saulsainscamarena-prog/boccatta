# Archive: Multimodule Migration

**Change:** multimodule-migration
**Status:** ARCHIVED

## Summary
The multimodule migration for Bocatta POS was successfully completed. The system transitioned from a monolithic `:app` structure to a layered, feature-based architecture (App, Feature, and Core layers). This transition enforced strict module boundaries, eliminated circular dependencies, and established clear architectural conventions for dependency injection and navigation.

## Key Decisions & Conventions Established

1. **Feature Module DI Decoupling:**
   - The monolithic `AppModules.kt` was dismantled.
   - Core dependencies (Repositories and UseCases) were moved to `DataModule.kt` in `:core:data`.
   - Feature modules now define their own Koin modules (e.g., `VentasModule.kt` for `:feature:ventas`), taking full ownership of their DI requirements.
   - `BocattaApp.kt` simply aggregates these decentralized modules during Koin initialization.

2. **Navigation Boundary Isolation:**
   - ViewModels are no longer passed as parameters from `AppNavGraph.kt`.
   - Features expose their navigation graphs via extension functions (e.g., `ventasScreen`).
   - Feature ViewModels are injected locally *inside* the feature navigation graphs using `koinViewModel()`, ensuring the `:app` module router remains ignorant of feature internals.
   - The App layer (`AppNavGraph`) orchestrates cross-feature routing exclusively through lambda callbacks.

## Final Status
All tests and compilations passed successfully without circular dependency errors. The migration fully satisfies the DAG constraints and dependency isolation goals.
