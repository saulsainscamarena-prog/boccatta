# Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`
Capability: `ventas-offline-sync`
Fecha: `2026-06-17`

## Problema

El cajero necesita que confirmar pago sea determinista. Hoy existen rutas donde el carrito puede quedar listo para doble cobro, una falla online no queda offline, el sync puede romperse con cliente y cancelar carrito no deja auditoria previa.

## Objetivos

- Garantizar un solo resultado de checkout: online, offline pendiente o rechazo operativo.
- Hacer idempotentes venta online y sync offline.
- Auditar cancelacion completa antes de limpiar carrito.
- Mantener cambios pequenos y verificables.

## No Objetivos

- No modificar formulas comerciales ni inventario.
- No cambiar dependencias, Gradle ni modulos.
- No redisenar UI administrativa.

## Usuarios y Flujos

### Flujo principal

1. Cajero cobra venta.
2. App persiste online o localmente.
3. App limpia carrito solo tras exito durable.

### Escenarios alternos

- Sin internet: venta se guarda local y queda sincronizable.
- Reintento/sync: venta existente remota no duplica inventario/lealtad.
- Error visible: operador ve mensaje estable, no stacktrace.
- Permisos/rol: no genera fallback offline si Firestore rechaza por permiso.

## Requisitos Funcionales

- R1: `SalesViewModelV2` limpia estado post-venta despues de exito durable.
- R2: `CheckoutUseCase` clasifica fallos y hace fallback offline solo en transitorios.
- R3: repositorio y sync evitan duplicar venta, folio, inventario o lealtad.
- R4: cancelacion de carrito conserva auditoria antes de limpiar.

## Requisitos No Funcionales

- Rendimiento de mostrador: no agregar esperas UI innecesarias.
- Persistencia/offline: todo exito local queda en cola.
- Auditabilidad: cancelaciones y sync mantienen IDs.
- Seguridad/permisos: errores crudos no se muestran al cajero.
- Accesibilidad: sin cambios visuales de alcance.

## Criterios de Aceptacion

- [ ] Tests unitarios relevantes pasan.
- [ ] `compileDebugKotlin` pasa.
- [ ] `git diff --check` pasa.

## Riesgos POS

- Ventas: doble cobro.
- Inventario: decremento duplicado.
- Offline/sync: deuda local congelada.
- Caja/pagos: boton confirmar sin resultado visible.
- Reportes/tickets: folio/codigo deben preservarse.

## Preguntas Abiertas

- Ninguna.
