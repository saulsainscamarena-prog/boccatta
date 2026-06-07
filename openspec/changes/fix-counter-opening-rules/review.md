# Review: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`

## Resultado

- Las reglas de apertura dejaron de bloquear jornada/mostrador para el usuario autenticado de prueba.
- `AperturaViewModelV2` ya no avanza silenciosamente cuando falla la asignacion inicial.
- `OperationalCatalogSyncRepository` refresca stock local aunque el fingerprint del catalogo ya este vigente.
- `OfflineDatabase` expone una consulta puntual para saber si un insumo existe antes de actualizar stock local.
- Checkout quedo protegido contra divergencia legacy de inventario:
  - lecturas remotas prefieren `cantidadEnBase`, luego `cantidadDisponible`, luego `currentQty`;
  - snapshots de `InventoryItem` normalizan la cantidad mostrada;
  - venta online y sync offline dejaron de decrementar `currentQty`, que es un campo legacy.

## Riesgos Restantes

- `currentQty` ya existente puede seguir sucio en documentos antiguos, pero deja de gobernar checkout.
- La administracion de stock de tienda sigue siendo confusa y esta anotada como pendiente separado: hace falta redisenar entrada/salida/ajuste de inventario para mostrador.
- El AVD no pudo completar una segunda venta post-fix por conflicto local de `adb` 1.0.40/1.0.41, no por error de compilacion.

## Decision

Mantener el cambio acotado. No abrir refactor amplio de inventario ni de AdminScreen dentro de esta spec.
