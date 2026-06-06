# Design: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`

## Punto de Entrada Real

- UI: `AperturaDiaScreen.ValidacionStockPremium`
- VM: `AperturaViewModelV2.confirmarAsignacion`
- Repo: `StockAllocationRepository.confirmarApertura`
- Caja: `CajaViewModel.abrirTurno`
- Seguridad remota: `firestore.rules`

## Decisiones

- Mantener el codigo Kotlin y alinear reglas con el flujo existente.
- Agregar helpers de reglas:
  - item vendible de apertura.
  - escritura de asignacion de apertura en branch portions.
  - movimiento legacy de inventario.
  - update operativo de sucursal.
- Mantener `sellerUpdatesStockDownOnly` para deducciones normales.

## Riesgo Principal

Permitir stock positivo desde vendedor. Se reduce restringiendo insumos a los cinco items de apertura y campos esperados.

