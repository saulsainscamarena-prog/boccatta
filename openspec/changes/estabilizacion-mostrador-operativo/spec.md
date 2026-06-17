# Estabilizacion integral de mostrador

Spec ID: `BCT-F15-MOSTRADOR-STABLE`
Capability: `counter_operation_stability`
Fecha: `2026-06-12`

## Problema

El vendedor necesita cobrar aun cuando la conectividad cambie durante la
transaccion. Administracion y soporte necesitan conocer toda la deuda offline,
recuperar fallos y auditar cancelaciones sin inspeccionar SQLite manualmente.

## Objetivos

- Garantizar persistencia durable de cada venta aceptada.
- Distinguir fallos transitorios de rechazos permanentes.
- Evitar duplicados mediante `forcedVentaId`.
- Hacer visible y accionable el estado de sincronizacion.
- Eliminar mensajes tecnicos de la UI operativa.
- Reducir pasos para actualizar stock desde administracion.

## No Objetivos

- Cambiar formulas, precios, promociones o reglas de stock.
- Cambiar dependencias o arquitectura de navegacion.
- Ocultar fallos permanentes mediante un fallback offline.

## Usuarios y Flujos

### Flujo principal

1. El vendedor confirma el pago una vez.
2. El sistema valida stock y persiste online o localmente.
3. La UI confirma venta remota o venta local pendiente.
4. La cola sincroniza y reconcilia sin duplicar inventario.

### Escenarios alternos

- Sin internet: persistencia SQLite atomica y ticket local.
- Reintento/sync: mismo ID, backoff y reconciliacion del ticket remoto.
- Error visible: mensaje operativo, detalle tecnico solo en Timber/diagnostico.
- Permisos/rol: rechazo visible, sin crear venta offline.

## Requisitos Funcionales

- R1: Checkout debe clasificar el fallo remoto antes de decidir fallback.
- R2: Fallo transitorio debe guardar local con el mismo `forcedVentaId`.
- R3: Fallo de stock, permisos, auth o datos debe permanecer rechazado.
- R4: Sync debe detectar venta remota existente y reconciliar ticket antes de
  marcar la venta local sincronizada.
- R5: Fallos transitorios no deben convertirse automaticamente en terminales.
- R6: El resumen offline debe incluir ventas pendientes, ventas fallidas,
  operaciones, ajustes de stock y turnos de contingencia.
- R7: Errores al leer el resumen deben ser observables, no equivaler a cero.
- R8: La cancelacion total debe guardar motivo, usuario, sucursal, items, total
  y estado de aprobacion antes de limpiar el carrito.
- R9: El boton de efectivo exacto debe usar el total decimal real; chips de
  denominacion deben tener comportamiento consistente.
- R10: Administrar tienda debe ofrecer acceso directo a stock, produccion,
  ajustes y alertas sin recorrer categorias de configuracion.

## Requisitos No Funcionales

- Rendimiento de mostrador: una confirmacion no debe lanzar dos coroutines.
- Persistencia/offline: venta, folio y descuento local deben ser atomicos.
- Auditabilidad: toda transicion debe conservar ID, intento y causa.
- Seguridad/permisos: solo roles autorizados cancelan o reintentan.
- Accesibilidad: controles tactiles de al menos 48 dp y etiquetas claras.

## Criterios de Aceptacion

- [ ] Given red validada, when Firestore responde UNAVAILABLE, then la venta
  queda pendiente local y la UI confirma modo offline.
- [ ] Given PERMISSION_DENIED, when se confirma pago, then no se guarda venta y
  se muestra instruccion para soporte/admin.
- [ ] Given una venta remota ya creada con el mismo ID, when SyncWorker la
  procesa, then no descuenta stock de nuevo y reconcilia el ticket.
- [ ] Given tres timeouts, when WorkManager reintenta, then la venta sigue
  pendiente y visible, no terminal.
- [ ] Given una cancelacion total, when se confirma NIP, then existe una
  operacion durable antes de vaciar el carrito.
- [ ] Given deuda offline mixta, when se abre ventas, then el encabezado muestra
  el total y diferencia pendientes de fallos.
- [ ] Given total de 40.50, when se toca efectivo exacto, then pagaCon es 40.50.
- [ ] Given un administrador, when abre Administrar tienda, then llega a
  actualizar stock en un maximo de dos acciones.

## Riesgos POS

- Ventas: commit remoto ambiguo.
- Inventario: doble deduccion en reintento.
- Offline/sync: registros terminales ocultos.
- Caja/pagos: doble toque y feedback tardio.
- Reportes/tickets: ticket local y remoto diferentes hasta reconciliacion.

## Preguntas Abiertas

- El diseño visual final de administracion requiere revision humana en AVD
  despues de estabilizar las fases criticas.
