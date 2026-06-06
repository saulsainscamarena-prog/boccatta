# Research: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`

## Contexto

No requiere investigacion web. El AVD real mostro `PERMISSION_DENIED` al comenzar jornada despues de confirmar asignacion y capturar fondo de caja.

## Evidencia Local

- AVD: `bocatta_tablet_api36`
- Pantalla: `INICIO DE JORNADA` -> `FONDO DE CAJA`
- Error visible: `Error: PERMISSION_DENIED: Missing or insufficient permissions.`
- Logcat: `Write failed at v2_inventory_branch_portions/atlixco_masa_crepa`

## Causa

`StockAllocationRepository` escribe asignaciones positivas de stock vendible al abrir turno, pero `firestore.rules` solo permite a vendedores crear deducciones de stock. La apertura tambien actualiza `v2_sucursal_config`, actualmente solo escribible por admin.

