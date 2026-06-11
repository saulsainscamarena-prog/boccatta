# Delta for ViewModel Blocking Fix and Edge-to-Edge Support

## ADDED Requirements

### Requirement: Asynchronous ViewModel Loading

The system **MUST** initialize ViewModel data asynchronously to avoid blocking the main thread.

#### Scenario: ViewModel loads data without blocking UI
- **GIVEN** a ViewModel (e.g., `CajaViewModel`) is created
- **WHEN** the ViewModel's `load()` method is called
- **THEN** the data loading operations execute on `Dispatchers.IO`
- **AND** the UI remains responsive with no frame drops.

### Requirement: Edge-to-Edge Scaffold

The system **MUST** provide a reusable `EdgeToEdgeScaffold` composable that applies `WindowInsets.safeDrawing` to its content area.

#### Scenario: Screen uses EdgeToEdgeScaffold
- **GIVEN** any screen composable (e.g., `SalesScreen`)
- **WHEN** it is wrapped with `EdgeToEdgeScaffold`
- **THEN** the content respects system insets (status bar, navigation bar) and renders edge‑to‑edge correctly.

## MODIFIED Requirements

### Requirement: Existing ViewModel Init Blocks

The previous specification required ViewModels to perform data loading in the `init{}` block synchronously, which **SHOULD NOT** block the main thread. (Previously: synchronous calls in `init{}` caused UI jank.)

#### Scenario: Updated ViewModel initialization
- **GIVEN** a ViewModel such as `AdminViewModel`
- **WHEN** its `load()` method is invoked from the UI layer (e.g., `viewModel.load()`)
- **THEN** the method launches a coroutine in `viewModelScope` on `Dispatchers.IO`
- **AND** any `runBlocking` calls have been replaced with suspend functions.

### Requirement: Edge-to-Edge Usage Across Screens

Previously only `MainActivity` called `enableEdgeToEdge()` and only `HeldOrdersScreen` and `InventoryScreen` handled insets. The system **SHOULD** ensure all screens consume `WindowInsets.safeDrawing` via `EdgeToEdgeScaffold`. (Previously: many screens ignored insets, causing UI clipping.)

#### Scenario: Updated screens use EdgeToEdgeScaffold
- **GIVEN** any of the listed screen composables (e.g., `ClientesScreen`)
- **WHEN** the screen is displayed
- **THEN** it is wrapped with `EdgeToEdgeScaffold`
- **AND** the UI respects safe drawing insets on all devices.

## REMOVED Requirements

### Requirement: Synchronous init{} data loading in ViewModels

(Reason: Deprecated due to main‑thread blocking)
(Migration: Replaced by asynchronous `load()` methods.)

### Requirement: Direct calls to `enableEdgeToEdge()` in activity only

(Reason: Incomplete UI inset handling)
(Migration: Centralized via `EdgeToEdgeScaffold` composable.)

## RENAMED Requirements

### Requirement: ViewModel Data Loading → Asynchronous ViewModel Loading

(Reason: Clarifies the non‑blocking nature of the operation)
(Migration: Update documentation and tests to reference new requirement name.)

### Requirement: Edge Insets Handling → Edge-to-Edge Scaffold

(Reason: Introduces reusable composable component.)
(Migration: Update all screen implementations and related tests.)