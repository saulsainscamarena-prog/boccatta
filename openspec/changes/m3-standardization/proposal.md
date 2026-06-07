# Proposal: Material 3 Color Migration

## Overview
Based on the findings in `research.md`, this proposal outlines the mapping from hardcoded `Color` instances to dynamic `MaterialTheme.colorScheme` tokens. This ensures that the app adapts seamlessly to Dark Mode and adheres to Android/Material 3 guidelines.

## Proposed Mappings

### 1. `admin/AdminScreen.kt`
Currently uses specific hex values for dashboard cards to differentiate them. We propose mapping them to standard semantic/tertiary M3 tokens:
- `Color(0xFFE91E63)` (Pink) -> `MaterialTheme.colorScheme.tertiary` or `MaterialTheme.colorScheme.error` (depending on UI semantics; `tertiary` provides a vibrant accent).
- `Color(0xFFFF9800)` (Orange) -> `MaterialTheme.colorScheme.secondary` or a custom extension color if necessary. Since we are standardizing to M3, we will use `MaterialTheme.colorScheme.secondary`.
- `Color(0xFF9C27B0)` (Purple) -> `MaterialTheme.colorScheme.primary` or `MaterialTheme.colorScheme.tertiaryContainer` to maintain distinction from the others.

### 2. `admin/GestionSucursalesScreen.kt`
- `Color.White.copy(0.7f)` -> `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)` (or `onPrimary` depending on its container's background color. Assuming it's inside a surface/card).

### 3. `admin/TabApariencia.kt`
This screen has color pickers and UI previews. We must ensure we don't break the color picker logic, but standard UI elements should use tokens:
- `Color.Gray` (Fallback background) -> `MaterialTheme.colorScheme.outline` or `MaterialTheme.colorScheme.surfaceVariant`.
- `Color.White.copy(alpha = 0.92f)` -> `MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)` or `MaterialTheme.colorScheme.onPrimary`.
- `Color.Black.copy(alpha = 0.65f)` -> `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)`.
