# Plan: Fix Bocatta POS Audit Criticals

## Goal
Resolve the four critical/high issues identified in the Bocatta POS audit so the project compiles, runs, and has a consistent architecture.

## Current State (Re-assessment, May 18)

I inspected every relevant file. Some audit findings have already been partially addressed:

| # | Issue | Audit Severity | Current State | Needs Work? |
|---|-------|----------------|---------------|-------------|
| 1 | DI: PromotionsEngineV2 vs PromocionesEngine | CRITICAL | Both registered in AppModules (lines 81-82). Works but redundant. `SalesFlowUseCase`/`ProductionBatchUseCase` use `PromotionsEngineV2` directly (not via DI). `SalesViewModelV2` uses `PromocionesEngine` via DI. | **Yes** — consolidate |
| 2 | CI uses JDK 11 for Kotlin 2.2.10 | HIGH | CI `.github/workflows/ci.yml` step name says "Set up JDK 11" (line 17) but `java-version: 17` (line 21). Actually already correct at runtime. | **Minor** — fix step name only |
| 3 | ~40 compile errors from deleted theme tokens | CRITICAL | `StatusBadgePremium` IS defined in `BocattaComponentsV2.kt` line 137. All screens use `import ...components.*` wildcard. `BocattaNeonCyan` not referenced anywhere. Likely already resolved. | **Verify** — attempt build |
| 4 | gradle.properties has "Error: Please select Android SDK" | HIGH | File is clean (35 lines, no artifact text). | **None** — already fixed |

## Step-by-step Plan

### Phase 1: CI Step Name (trivial)
- **File**: `.github/workflows/ci.yml`
- **Change**: Line 17: rename `"Set up JDK 11"` → `"Set up JDK 17"`
- **Verification**: Visual diff

### Phase 2: Consolidate Dual Engine (PromocionesEngine + PromotionsEngineV2)
This is the main architectural cleanup. Both engines exist with overlapping responsibility:
- `PromocionesEngine` (class) — Spanish API, `calcular(carrito, promociones)`, used via DI in `SalesViewModelV2`
- `PromotionsEngineV2` (object) — English API, `calculate(subtotal, itemCount, ...)`, used directly in `SalesFlowUseCase` and `ProductionBatchUseCase`

**Approach**: Consolidate into `PromotionsEngineV2` (it has richer discount logic — BUY_X_GET_Y, FIXED, PERCENTAGE — while `PromocionesEngine` only has porcentaje/monto/price-fixed). Then make it injectable via DI everywhere.

**Files to change**:

1. **`PromotionsEngineV2.kt`** (domain/usecase)
   - Add missing Spanish-level features from `PromocionesEngine` if any (check: `alcance`/scope filtering — `TICKET_COMPLETO`, `PRODUCTOS_ESPECIFICOS`, `CATEGORIAS_ESPECIFICAS`)
   - After consolidation, this becomes THE single engine

2. **`PromocionesEngine.kt`** (domain/usecase)
   - Mark `@Deprecated("Use PromotionsEngineV2 instead")` with a `DeprecatedReplaceWith` annotation pointing to `PromotionsEngineV2`
   - Keep the file but forward calls to `PromotionsEngineV2`

3. **`SalesFlowUseCase.kt`** (domain/usecase)
   - Change from direct `PromotionsEngineV2 = PromotionsEngineV2` constructor default to accepting via DI: `private val promotionsEngine: PromotionsEngineV2`
   - Update AppModules registration accordingly

4. **`ProductionBatchUseCase.kt`** (domain/usecase)
   - Same change: accept `PromotionsEngineV2` via constructor, not default

5. **`SalesViewModelV2.kt`** (presentation/viewmodel)
   - Already uses `PromocionesEngine` from `deps` — change to `PromotionsEngineV2` when the rename propagates through `SalesDependencies`

6. **`SalesDependencies.kt`** (di)
   - Change `promocionesEngine: PromocionesEngine` → `promotionsEngine: PromotionsEngineV2`

7. **`AppModules.kt`** (di)
   - Remove redundant double registration (line 81-82)
   - Keep `single { PromotionsEngineV2 }` only
   - Update `SalesDependencies` construction to pass `promotionsEngine = get()`

8. **Test files**:
   - `PromocionesEngineTest.kt` — migrate tests to target `PromotionsEngineV2` or mark deprecated
   - `KoinModuleTest.kt` — update `PromocionesEngine` references to `PromotionsEngineV2`
   - `SalesFlowUseCaseTest.kt` — update constructor call

**Risks & Mitigation**:
- `PromocionesEngine` has `AlcancePromo` scope filtering (`TICKET_COMPLETO`, `PRODUCTOS_ESPECIFICOS`, `CATEGORIAS_ESPECIFICAS`) not in `PromotionsEngineV2`. Must port these concepts before deprecating.
- `PromotionsEngineV2` has `DiscountV2.conditions` map-based conditions — ensure feature parity.
- The `calcular()` signature takes `carrito` (list of items) while `calculate()` takes subtotal + itemCount + categories. These are different abstractions — may need a new overload in `PromotionsEngineV2` that accepts `carrito`.

### Phase 3: Build Verification
- Run: `./gradlew assembleDebug`
- Run: `./gradlew testDebugUnitTest`
- If there are remaining theme token errors (unlikely based on current state), locate the broken token references and add them back to the theme files.

### Phase 4: (Optional) CI detekt/ktlint cleanup
- Run `./gradlew detekt` and `./gradlew ktlintCheck` to verify CI passes
- Fix any linter issues

---

## Files Likely to Change (definitive list)

| File | Change |
|------|--------|
| `.github/workflows/ci.yml` | Step name typo fix |
| `domain/usecase/PromotionsEngineV2.kt` | Add scope/carrito overload |
| `domain/usecase/PromocionesEngine.kt` | Mark @Deprecated |
| `domain/usecase/SalesFlowUseCase.kt` | DI injection instead of direct init |
| `domain/usecase/ProductionBatchUseCase.kt` | DI injection instead of direct init |
| `di/SalesDependencies.kt` | Rename field to `PromotionsEngineV2` |
| `di/AppModules.kt` | Remove duplicate, update wiring |
| `presentation/viewmodel/SalesViewModelV2.kt` | Update field type |
| `test/.../PromocionesEngineTest.kt` | Migrate to PromotionsEngineV2 |
| `test/.../KoinModuleTest.kt` | Update references |
| `test/.../SalesFlowUseCaseTest.kt` | Update constructor call |

## Verification Steps
1. `./gradlew assembleDebug` — must succeed
2. `./gradlew testDebugUnitTest` — all tests green
3. `./gradlew detekt` — no new issues
4. Manual smoke test: launch app, verify sales flow works without Koin crash

## Open Questions
1. Should `PromocionesEngine` be fully removed (deleted file) or just deprecated (kept for reference)?
2. The scope/alcance features in `PromocionesEngine` — do they have equivalents in `PromotionsEngineV2` or do we need to extend `PromotionsEngineV2` to support them?
3. Is there a CI runner that actually has JDK 17 available, or do we need to update the Gradle wrapper properties too?
