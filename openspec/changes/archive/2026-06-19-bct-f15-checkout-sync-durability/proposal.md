# Proposal: Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`
Capability: `ventas-offline-sync`
Strict TDD: `parcial`

## Problema

La ruta de mostrador todavia puede dejar una venta en estado ambiguo: carrito sin limpiar tras exito, fallo online transitorio sin fallback offline, sync que puede fallar por lecturas Firestore despues de escrituras y cancelaciones de carrito no auditables.

## Soft Gate SDD

Hay mas de 3 cambios SDD abiertos. Se ignora el gate porque este cambio corrige riesgos P0/P1 de mostrador real y no introduce feature nueva; posponerlo mantiene riesgo de doble cobro o venta no durable.

## Alcance

- Estado post-venta en `SalesViewModelV2`.
- Fallback durable en `CheckoutUseCase`.
- Idempotencia y orden de transacciones en `FirebaseSalesRepositoryV2` y `SyncWorker`.
- Cancelacion completa de carrito por `RegistrarCancelacionUseCase`.
- Tests unitarios/contratos relevantes.

## Fuera de Alcance

- Gradle, dependencias, Play Store, Windows port.
- Redisenos de administrar tienda.
- Cambios de precios, recetas, promociones o formulas de inventario.

## Archivos Afectados Esperados

- ViewModel/use cases/repositorios/sync del flujo de venta.
- DI para inyectar cancelacion.
- Tests existentes de checkout/cancelacion/sync.
- `notas.txt` y artefactos SDD.

## Riesgos

- Ventas: evitar doble cobro y venta perdida.
- Inventario: no cambiar calculo de deducciones.
- Offline/sync: mantener ventas pendientes recuperables.
- Caja/pagos: conservar bloqueo de doble toque.
- UI: no redisenar, solo mensajes/estado.
- Seguridad: no exponer errores crudos.

## Research Externo

- Web requerida: no
- Pregunta investigada: no aplica
- Fuentes primarias: no aplica
- Decision tomada: repo local basta
- Suposicion sensible a version: no

## Estrategia de Rollback

Revertir este cambio completo; no hay migraciones de datos ni Gradle.

## Criterios de Exito

- [ ] Venta exitosa limpia carrito y no duplica lealtad.
- [ ] Fallo transitorio online se guarda offline con el mismo ID.
- [ ] Sync no marca transitorios como criticos por intentos.
- [ ] Cancelacion completa se audita antes de limpiar.
