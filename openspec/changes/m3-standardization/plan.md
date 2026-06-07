# Plan: Material 3 Refactoring Execution

## Prerequisites
- Confirm that the proposed color tokens visually align with the intended UI design in light and dark themes.

## Execution Steps

### Step 1: Update `admin/AdminScreen.kt`
- Replace hardcoded hex colors in `DashboardCardPremium` instantiations.
  - Line 235 (Personal): Change `Color(0xFFE91E63)` to `MaterialTheme.colorScheme.tertiary`
  - Line 244 (Reportes): Change `Color(0xFFFF9800)` to `MaterialTheme.colorScheme.secondary`
  - Line 253 (Ajustes): Change `Color(0xFF9C27B0)` to `MaterialTheme.colorScheme.primary`

### Step 2: Update `admin/GestionSucursalesScreen.kt`
- Target: Line 140
  - Change: `color = Color.White.copy(0.7f)`
  - To: `color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)` (or context-appropriate on-color).

### Step 3: Update `admin/TabApariencia.kt`
- Target: Line 252 & 469 (Fallback colors)
  - Change: `Color.Gray`
  - To: `MaterialTheme.colorScheme.outline`
- Target: Line 354
  - Change: `color = Color.White.copy(alpha = 0.92f)`
  - To: `color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)`
- Target: Line 360
  - Change: `color = Color.Black.copy(alpha = 0.65f)`
  - To: `color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)`

### Step 4: Verification
- Compile and build the project `.\gradlew.bat compileDebugKotlin` to ensure no syntax errors.
- Ensure no `.kt` file changes are committed yet as per user instructions.
- Present the SDD to the user/parent agent for review before proceeding with `.kt` code edits.
