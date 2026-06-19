# Design: Sistema UX/UI, navegacion y color

Spec ID: `ux-ui-navigation-color-system`

## Resumen Tecnico

Un `CompositionLocal` proveera colores semanticos desde `M3Theme`. Ventas reutilizara una lista unica de destinos para drawer movil y rail tablet. Los componentes usaran roles `on*` coherentes. Admin reemplazara grid fijo por `GridCells.Adaptive` y scroll natural.

## Dependencias de Documentacion Externa

- No aplica: componentes presentes y skills locales recientes.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio tecnico | Prueba |
|-----------|--------------|----------------|--------|
| R1 | `core/ui/theme` | tokens + provider | test Compose |
| R2 | `InventoryScreen` | roles M3, strings, FAB | compile/test |
| R3-R4 | `SalesScreen*` | rail/drawer compartidos | test Compose |
| R5 | `AdminScreen` | grid adaptativo | compile/test |
| R6-R7 | componentes/pantallas | 48 dp/semantica | Compose/inspeccion |

## APIs, Contratos y Datos

- Dominio, repositorios, DI, Firestore, Room y WorkManager: sin cambios.
- API UI: `MaterialTheme.bocattaSemanticColors`.

## Edge Cases

- Sin internet: texto/color offline visibles.
- Reconexion: success estable.
- Doble toque: manejo actual sin cambios.
- Rol no autorizado: destinos Admin ocultos.

## Guardrails Aplicados

- [x] Negocio fuera de Composables.
- [x] Sin cambios de repositorios/use cases.
- [x] Offline e inventario funcional intactos.

## Checkpoint Humano

Aprobado por "hazlo todo".
