# Research: Material 3 Standardization (Rest of Application)

## Goal
Identify hardcoded colors (e.g., `Color.White`, `Color.Black`, `Color.Gray`, and Hex colors) across the application screens, excluding the already standardized Inventory and Financial screens. Ensure compliance with the Material 3 design system by mapping these hardcoded values to `MaterialTheme.colorScheme` tokens.

## Scope Exclusions
The following directories correspond to Inventory and Financial features and are excluded from this analysis:
- `inventario`
- `caja`
- `compras`
- `gastos`
- `bodega`

## Findings
We ran a search across `app/src/main/java/com/bocatta/pos/presentation/ui/screens/` excluding the above directories. The results yielded only a few files that still contain hardcoded colors (ignoring `Color.Transparent` which is theming-compliant):

1. **`admin/AdminScreen.kt`**
   - Line 235: `color = Color(0xFFE91E63)` (Used for a Dashboard card "Personal")
   - Line 244: `color = Color(0xFFFF9800)` (Used for a Dashboard card "Reportes")
   - Line 253: `color = Color(0xFF9C27B0)` (Used for a Dashboard card "Ajustes")

2. **`admin/GestionSucursalesScreen.kt`**
   - Line 140: `color = Color.White.copy(0.7f)` (Used for a subtitle text)

3. **`admin/TabApariencia.kt`**
   - Line 252: `Color.Gray` (Fallback background color)
   - Line 354: `color = Color.White.copy(alpha = 0.92f)`
   - Line 360: `color = Color.Black.copy(alpha = 0.65f)`
   - Line 469: `Color.Gray` (Fallback background color)
