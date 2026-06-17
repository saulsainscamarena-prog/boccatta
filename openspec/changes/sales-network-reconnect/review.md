# Review: Reconectar estado online del mostrador

Spec ID: `BCT-F14-NETWORK`

## Resultado

- El estado `isOnline` ya tiene un unico owner en `SalesViewModelV2`.
- El collector usa `viewModelScope` y no introduce listeners manuales.
- No se modificaron checkout, inventario, sync, DI ni navegacion.
- El cambio compila de forma aislada en `feature:ventas`.

## Riesgo residual

- Falta validar en AVD que el encabezado cambie al cortar y restaurar red.
- La APK completa no puede generarse por un cambio concurrente en
  `CarritoPanelV2.kt:169` que no resuelve `Icons.Default.Star`.
