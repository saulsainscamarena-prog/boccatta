# Proposal: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`
Capability: `counter-opening`
Strict TDD: `no`

## Problema

- El mostrador no puede abrir jornada online porque Firestore rechaza escrituras que la app necesita para asignar stock vendible y marcar la sucursal abierta.

## Alcance

- Ajustar `firestore.rules` para permitir a vendedor autenticado ejecutar la apertura operativa ya implementada.
- Mantener restricciones por campos y colecciones.
- Corregir el rol `DUEÑO` en reglas para no romper usuarios dueno.

## Fuera de Alcance

- Redisenar inventario/admin.
- Cambiar modelos Kotlin o repositorios.
- Cambiar flujo de ventas o sync.

## Archivos Afectados Esperados

- `firestore.rules`

## Riesgos

- Ventas: bajo, no toca checkout.
- Inventario: medio, permite asignacion positiva solo en items vendibles de apertura.
- Offline/sync: bajo, no toca colas.
- Caja/pagos: medio, desbloquea apertura de turno.
- UI: nulo.
- Seguridad: medio, requiere reglas estrechas por campos y documentos.

## Criterios de Exito

- [ ] Apertura online no falla por `v2_inventory_branch_portions`.
- [ ] Apertura online no falla por `v2_sucursal_config`.
- [ ] AVD llega a mostrador.

