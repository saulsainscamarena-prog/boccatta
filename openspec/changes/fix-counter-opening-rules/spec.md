# Spec: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`

## Requisitos

- Given un usuario autenticado con rol operativo, When abre jornada y confirma asignacion de stock, Then puede crear/actualizar `v2_inventory_branch_portions` solo para items vendibles de apertura.
- Given la apertura termina, When se marca sucursal abierta, Then puede crear/actualizar solo campos operativos de `v2_sucursal_config`.
- Given el usuario cierra caja, When se marca sucursal cerrada, Then puede actualizar `abierta=false`.
- Given un vendedor intenta escritura administrativa no operativa, Then sigue denegada.

## No Objetivos

- No permitir compras, proveedores, nomina ni administracion general a vendedores.
- No permitir borrados.
- No cambiar deducciones de ventas.

