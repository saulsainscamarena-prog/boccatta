# Research: Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`
Capability: `ventas-offline-sync`
Fecha: `2026-06-17`

## Brief Humano

Estabilizar mostrador real: una venta confirmada debe quedar online, offline sincronizable o rechazada con causa operativa. No se toca Play Store, Gradle, precios, recetas ni redisenos amplios.

## Web Research Gate

- Research question: no aplica.
- Source category from `openspec/source-catalog.yaml`: no aplica.
- Why local context is insufficient: el cambio corrige contratos locales ya auditados.
- Expected decision: reutilizar use cases, repositorios, OfflineManager y SyncWorker existentes.
- Stop condition: lectura de owners actuales completada.
- Budget: none.

Si `Budget` es `none`, explicar por que el repo local basta: las decisiones dependen de codigo local actual y reglas SDD ya leidas.

## Contexto Local Leido

- Constitution: `openspec/constitution.md`
- AGENTS: `AGENTS.md`
- Skill registry: skills Android de arquitectura, Room/offline, WorkManager y Compose state.
- Source catalog: no requerido.
- Archivos inspeccionados: `SalesViewModelV2`, `CheckoutUseCase`, `FirebaseSalesRepositoryV2`, `SyncWorker`, `SyncScheduler`, `OfflineManager`, `RegistrarCancelacionUseCase`, DI y tests existentes.

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|

## Alternativas

### Opcion A

- Descripcion: corregir solo UI post-venta.
- Ventajas: pequena.
- Costos: deja ventas perdibles durante fallos online y sync fragil.
- Riesgos: mostrador no certificado.

### Opcion B

- Descripcion: estabilizar checkout, repositorio, sync y cancelacion usando piezas existentes.
- Ventajas: cierra riesgos P0/P1 sin arquitectura nueva.
- Costos: toca contratos criticos y requiere tests.
- Riesgos: errores de compatibilidad si no se actualizan tests/DI.

## Decision Recomendada

Aplicar opcion B con cambios quirurgicos, sin alterar Gradle ni deducciones.

## Suposiciones Externas

Ninguna.

## Tradeoffs

La venta online sigue siendo preferida, pero un fallo transitorio se conserva offline para proteger operacion.

## Preguntas Que Deben Aclararse Antes de Proponer

Ninguna.
