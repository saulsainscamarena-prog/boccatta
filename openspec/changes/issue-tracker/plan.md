# Plan - Issue Tracker Fix Execution

## Objective
Execute the prioritized fixes identified in the proposal without modifying the `domain/` or `data/` layers. 

## Step-by-Step Execution Plan

### Step 1: Baseline and Setup
- [ ] Verify or initialize Git repository (`git init`, `git add .`, `git commit -m "snapshot seguro pre-corrección-ux-ops"`).
- [ ] Run `./gradlew testDebugUnitTest` to establish a passing baseline (expecting 218 tests passing).

### Step 2: Resolve Functional Blockages (State & UI Logic)
- [ ] **SalesViewModelV2.kt**: 
  - Update `escucharMenu()` to assign `catalogoProcesado`.
  - Capture `heldOrderId` and `mesaId` securely during `aplicarResultadoVenta()` to prevent premature cleanup.
- [ ] **SalesScreen.kt**:
  - Update the held orders dialog logic to load the order without deleting it.
  - Fix the re-reservation race condition by ensuring order replacement does not free the table incorrectly.

### Step 3: Implement UX and Presentation Feedback
- [ ] **SalesViewModelV2.kt**: Wrap `onCobrar()` in a `try/catch`. Update the `SalesUiState` to include `isLoading`, `error`, and `showSuccess`.
- [ ] **SalesScreen.kt**: Add `CircularProgressIndicator` and `LaunchedEffect` Snackbar triggers for `error` and `showSuccess`.
- [ ] **ProductionRegistrationDialog.kt**: Remove raw material inputs; implement a single numeric yield input field.
- [ ] **DynamicFormEngine.kt**: Add fallback decoding and regex to clean UTF-8 characters from labels.
- [ ] **CrepeBuilderDialog.kt**: Replace single-choice elements with `FlowRow` and `FilterChip` for multiselection, and update state handling.

### Step 4: Verification
- [ ] Run `./gradlew testDebugUnitTest` to ensure no domain/data tests were broken.
- [ ] Run `./gradlew compileDebugKotlin` to ensure syntax correctness.
- [ ] (Manual/Operator) Verify sales flows with Snackbars, crepe multiselection, and held order preservation.

### Step 5: Address Remaining Tech Debt (Future Iteration)
- [ ] Migrate ViewModels to use `IProductRepository.getAllProducts()`.
- [ ] Load `product_definitions` from Firestore for `DynamicFormEngine`.
- [ ] Triage and resolve the ~261 `TODO` comments logged in the codebase.
