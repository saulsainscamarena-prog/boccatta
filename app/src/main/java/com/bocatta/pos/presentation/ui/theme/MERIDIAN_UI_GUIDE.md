# Meridian Design System — Bocatta POS

## Tokens de Color (MaterialTheme.colorScheme)

| Token | Light | Dark | Uso |
|-------|-------|------|-----|
| `primary` | `#1B5E20` | `#4CAF50` | Botones principales, precios, acentos |
| `onPrimary` | `#FFFFFF` | `#000000` | Texto sobre primary |
| `secondary` | `#FF6F00` | `#FFB74D` | Acentos secundarios, badges |
| `onSecondary` | `#FFFFFF` | `#000000` | Texto sobre secondary |
| `error` | `#C62828` | `#EF5350` | Errores, stock agotado, cancelar |
| `background` | `#F5F6F8` | `#08090F` | Fondo general |
| `surface` | `#FFFFFF` | `#121212` | Tarjetas, diálogos, superficies |
| `onSurface` | `#212121` | `#E0E0E0` | Texto principal |
| `onSurfaceVariant` | `#757575` | `#9E9E9E` | Texto secundario, etiquetas |
| `outline` | `#BDBDBD` | `#616161` | Bordes, divisores |
| `outlineVariant` | `#E0E0E0` | `#373737` | Bordes suaves, separadores |

**Regla estricta**: No usar `Color.White`, `Color.Black`, `Color.Gray` ni colores hex hardcodeados en la capa `presentation/`. Todo debe usar `MaterialTheme.colorScheme.*`.

## Tipografía

| Elemento | Size | Weight | Uso |
|----------|------|--------|-----|
| Precio (grande) | `20sp` | `SemiBold` | Precio en tarjeta de producto |
| Precio (muy grande) | `24-36sp` | `Black` | Total a pagar |
| Nombre producto | `16sp` | `SemiBold` | Nombre en tarjeta (max 2 líneas, ellipsis) |
| Título sección | `15-18sp` | `Black` | Encabezados de panel |
| Cuerpo | `14sp` | `Normal` | Texto general |
| Etiqueta/metadata | `12-13sp` | `Normal` | Unidades, stock, info secundaria |
| Mínimo absoluto | `10sp` | - | Badges, sellos (solo decorativo) |

**Regla**: Ningún texto funcional < `14sp`. Badges y sellos pueden ser `10-12sp`.

## Espaciado (Grid 8dp)

| Múltiplo | Uso típico |
|----------|-----------|
| `4dp` | Padding mínimo, separación interna de badges |
| `8dp` | Espaciado entre elementos relacionados |
| `12dp` | Padding de cards, separación entre secciones |
| `16dp` | Padding estándar de pantalla |
| `20dp` | Separación entre secciones mayores |
| `24dp` | Padding de diálogos, separación de grupos |
| `32dp` | Espaciado grande entre secciones |

## Radios de Borde

| Componente | Radio |
|-----------|-------|
| Cards de producto | `28dp` (píldora) |
| Cards de contenido | `12-20dp` |
| Botones | `14-26dp` |
| Inputs / TextFields | `8-16dp` |
| Badges / Chips | `4-8dp` |
| Diálogos | `24dp` |

## Touch Targets

- Mínimo: `48dp x 48dp` para elementos interactivos
- IconButtons: `36dp` mínimo (excepción para iconos en listas)
- Botones de acción fijos: `52dp` altura

## Layout Adaptativo

Breakpoint: `720dp`

### Teléfono (<720dp)
- Catálogo: `Column` con `weight(1f)`
- Carrito: `ModalBottomSheet` (50% altura máxima)
- Botón cobrar: `BottomAppBar` fijo

### Tablet/POS (>=720dp)
- Catálogo: `Modifier.weight(0.62f)`
- Carrito: `Modifier.weight(0.38f)` con `requiredWidthIn(320.dp, 480.dp)`
- Grid productos: `GridCells.Adaptive(160dp)`

## Componentes Reutilizables

Todos en `BocattaComponents.kt` / `BocattaComponentsV2.kt`:

- `BocattaSearchBar` — con debouncing 300ms
- `BocattaCartItemRow` — item de carrito con precio y botón eliminar
- `ProductCardV2` — tarjeta cuadrada 1:1 con nombre, precio, badge stock
- `BocattaButton` — botón primario con estado de carga
- `BocattaMetricCard` — tarjeta de métrica para reportes
- `BocattaBadge` — chip de estado
- `BocattaTopBar` — barra superior con título y navegación
- `BocattaEmptyState` — estado vacío con icono y texto
- `BocattaSectionTitle` — título de sección
- `BocattaFilaResumen` — fila etiqueta+valor

## Accesibilidad

- `contentDescription` en todos los `Icon` dentro de `IconButton`
- `contentDescription = null` solo para iconos decorativos (leadingIcon en TextField)
- Contraste mínimo 4.5:1 para texto normal, 3:1 para texto grande
- Touch targets mínimo 44dp (WCAG 2.5.8)

## Estructura de Archivos

```
presentation/
  ui/
    theme/          → Tokens, colores, tipografía
    components/     → Componentes reutilizables
    screens/        → Pantallas de la aplicación
  viewmodel/        → ViewModels
```
