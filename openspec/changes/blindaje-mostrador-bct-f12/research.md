# Research: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`
Capability: `mostrador-offline-sync-auth-ticket`
Fecha: `2026-06-06`

## Brief Humano

El usuario pidio continuar la fase de estabilizacion del mostrador real y revisar `AGENTS.md`, usando SDD. El contexto operativo prioriza que el POS pueda abrir turno, cobrar, generar tickets correctos, conservar operaciones offline y sincronizar sin bloquear el mostrador. Tambien hay trabajo paralelo sobre el port Windows, por lo que no se deben tocar Gradle, catalogos de versiones ni lanzar tareas de Gradle que se superpongan sin revisar procesos.

## Web Research Gate

- Research question: no aplica.
- Source category from `openspec/source-catalog.yaml`: no aplica.
- Why local context is insufficient: no es insuficiente; el problema se resuelve con reglas locales del proyecto, codigo actual y auditorias previas.
- Expected decision: aplicar correcciones quirurgicas en owners existentes.
- Stop condition: SDD completo y verificacion local documentada.
- Budget: none.

El repo local basta porque los cambios se apoyan en contratos internos: `AuthorizationManager`, `SyncWorker`, `IStockAdjustmentQueue`, `InventoryRepositoryImpl`, `CheckoutUseCase`, `CartManager`, `SalesViewModelV2` y generacion de ticket WhatsApp. No hay decision dependiente de version externa.

## Contexto Local Leido

- Constitution: `openspec/constitution.md`
- AGENTS: `AGENTS.md`
- Skill registry: `openspec/skill-registry.yaml`
- Source catalog: no se requirio web research.
- Skills usadas:
  - `android-app-architecture`
  - `android-background-work`
  - `android-room-offline-storage`
  - `android-user-identity-auth`
  - `android-testing-compose-room-workmanager`
- Archivos inspeccionados:
  - `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
  - `app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt`
  - `app/src/main/java/com/bocatta/pos/data/repository/InventoryRepositoryImpl.kt`
  - `app/src/main/java/com/bocatta/pos/domain/usecase/CheckoutUseCase.kt`
  - `app/src/main/java/com/bocatta/pos/domain/usecase/CartManager.kt`
  - `app/src/main/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCase.kt`
  - `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SalesViewModelV2.kt`
  - tests de auth, seguridad, offline operations y ticket.

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|
| No aplica | No aplica | 2026-06-06 | Contexto local | No se uso decision externa |

## Alternativas

### Opcion A

- Descripcion: refactor amplio de checkout, inventario, permisos y sync.
- Ventajas: permitiria limpiar limites de arquitectura acumulados.
- Costos: alto riesgo, mucho diff, mayor probabilidad de romper mostrador y conflicto con cambios paralelos.
- Riesgos: cambiar contratos criticos sin emulador/Gradle estable.

### Opcion B

- Descripcion: correcciones quirurgicas sobre owners actuales, con pruebas de contrato donde el harness existente lo permite.
- Ventajas: cambios pequenos, revisables y alineados a `AGENTS.md`.
- Costos: deja deuda arquitectonica pendiente.
- Riesgos: algunas garantias dependen de verificacion Gradle posterior.

## Decision Recomendada

Aplicar la opcion B. Esta fase no debe abrir redisenos amplios de admin/inventario, migraciones Room ni limpieza Gradle. Debe cerrar fallos concretos de mostrador y registrar la deuda restante en `notas.txt`.

## Suposiciones Externas

- El vendedor operativo necesita abrir turno sin depender de list queries a empleados bloqueadas por reglas.
- La cola offline de stock puede quedar en `SYNCING` por cierre abrupto o worker interrumpido.
- Los tickets compartidos por WhatsApp deben mostrar descuentos manuales para auditoria de caja.
- La verificacion Gradle debe respetar el trabajo paralelo del usuario y otro agente.

## Tradeoffs

- Se mantiene `CheckoutUseCase` con deuda de frontera dominio/data en lugar de refactor amplio.
- Se preserva el flujo actual de WorkManager y colas sin introducir nueva arquitectura.
- Se usan tests de contrato textuales para cubrir cambios donde fakes completos de Firebase/Room todavia no son practicos.

## Preguntas Que Deben Aclararse Antes de Proponer

- Ninguna bloquea esta fase. La UX de administracion de tienda y actualizacion de stock queda para una spec posterior.
