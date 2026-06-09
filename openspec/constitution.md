# Bocatta POS Constitution

Esta constitution es la fuente de reglas no negociables para Specs, planes, tareas e implementaciones asistidas por IA en Bocatta POS.

## Prioridades

1. Proteger la operacion de mostrador: cobrar rapido, evitar doble cobro y mantener la app usable en horas pico.
2. Preservar ventas offline: una venta debe poder guardarse, sincronizarse y auditarse aunque falle la red.
3. Mantener inventario correcto: no cambiar deducciones de stock sin validar venta online, venta offline y sincronizacion.
4. Respetar la arquitectura local: ViewModels para estado, use cases/repositorios para negocio, Koin para DI y Compose declarativo para UI.
5. Mantener cambios pequenos, auditables y verificables en Windows con `gradlew.bat`.

## Reglas de Spec

- Toda feature no trivial inicia con `research.md`, `proposal.md`, `spec.md`, `design.md`, `plan.md`, `tasks.md`, `review.md`, `verification.md` y `archive.md`.
- Antes de cargar contexto amplio, leer `openspec/skill-registry.yaml` y abrir solo las skills necesarias.
- Antes de buscar web, leer `openspec/web-research-policy.md` y `openspec/source-catalog.yaml`; buscar solo con pregunta, presupuesto y condicion de parada.
- La spec debe separar objetivos, no objetivos, criterios de aceptacion y riesgos.
- Los escenarios funcionales importantes deben expresarse con Given/When/Then.
- El plan debe identificar el punto de entrada real antes de proponer clases nuevas.
- Las tareas deben ser pequenas y comprobables; cada tarea critica debe apuntar a una verificacion.
- Si el alcance toca ventas, inventario, offline, sync, caja o pagos, la spec debe incluir escenarios online y offline.
- En strict TDD, escribir o ajustar tests antes del cambio funcional cuando exista un harness razonable.
- El review debe auditar cumplimiento contra proposal, spec, design y tasks antes de cerrar.
- Toda decision basada en informacion externa debe registrar fuente, fecha y version/alcance en `research.md`.

## Arquitectura Android

- La logica de negocio vive en dominio, use cases, repositorios o helpers existentes, no en Composables.
- Los ViewModels son la fuente de estado de UI.
- Los Composables renderizan estado y emiten eventos; no hacen IO, Firestore, Room, calculos pesados ni deducciones.
- Reutiliza modelos, repositorios, providers y modulos Koin existentes antes de crear abstracciones nuevas.
- Si cambias firmas de repositorios/use cases, actualiza contratos, DI y tests relacionados en la misma spec.

## Ventas, Inventario y Offline

- `SalesViewModelV2`, `InventoryDeductions`, `OfflineManager`, `SyncWorker`, colas offline y repositorios relacionados son zona critica.
- No describas el flujo offline como two-phase commit salvo que exista una implementacion real con ese contrato.
- No simplifiques deducciones de ingredientes sin demostrar waffles, crepas, frappes y combos con y sin internet.
- Protege checkout contra doble toque, corrutinas concurrentes y reintentos ambiguos.
- Todo cambio de stock debe conservar trazabilidad y recuperacion tras reconexion.

## Compose y UX de Mostrador

- Material 3, UI compacta, densa y legible para mostrador.
- Estado unidireccional y eventos consumidos despues de mostrarse.
- Modales se cierran al terminar exitosamente el flujo.
- Evita recomposiciones globales de la cuadricula de venta.
- Totales, filtros y calculos derivados deben vivir fuera de Composables pesados o usar estado derivado cuando aplique.
- Verifica movil y tablet cuando la pantalla sea de uso operativo.

## Integraciones y Seguridad

- No hardcodear secretos, endpoints privados ni credenciales.
- Revisar configuracion existente antes de inicializar Firebase, SDKs, hardware o APIs externas.
- Desacoplar perifericos POS mediante interfaces testeables cuando aplique.
- Liberar listeners, callbacks, observers y conexiones en el ciclo de vida correcto.

## Windows y Gradle

- Usar siempre `.\gradlew.bat`; no usar Gradle global.
- Mantener JDK 17 salvo migracion explicita y verificada.
- Si Gradle/Kotlin falla por locks o daemons en Windows, seguir el flujo de diagnostico de `AGENTS.md`.
- No borrar caches de Gradle ni wrapper como primera medida.

## Verificacion Minima

- Cambios Kotlin/Compose: `.\gradlew.bat compileDebugKotlin`.
- Dominio/repositorios/logica critica: unit tests relevantes, normalmente `.\gradlew.bat testDebugUnitTest`.
- Room/SQLite/offline persistente: tests instrumentados o prueba manual documentada si el entorno no permite emulador.
- UI de mostrador: prueba manual en movil/tablet o Compose UI test cuando exista harness.

## Governance Gates

1. **Soft Gate (3+ abiertos)**: Si hay 3 o más cambios SDD abiertos (sin archive.md), el `proposal.md` de cualquier cambio nuevo **DEBE** incluir una justificación obligatoria de por qué se ignora el gate. Esto se verifica ANTES de crear el cambio.

2. **Fases obligatorias**:
   - Proposal sin spec → **BLOQUEADO** (no se puede avanzar a design)
   - Spec sin design → **BLOQUEADO** (no se puede avanzar a tasks)
   - Design sin tasks → **BLOQUEADO** (no se puede avanzar a apply)
   - Tasks sin apply completado → **BLOQUEADO** (no se puede crear `verification.md`)

3. **Auto-archive (14 días)**: Cambios sin actividad (sin commits ni ediciones de artefactos) por 14 días consecutivos → se archiva automáticamente con estado "suspended" y un `archive.md` explicando el motivo.

4. **Gate offline**: Cualquier cambio que toque ventas, inventario o sync **DEBE** incluir verificación offline en `verification.md`. Sin esa verificación, el `verification.md` no es válido.

5. **Beta Gate**: Los 6 flujos críticos (checkout, offline sync, inventario, caja, reportos, ventas) deben tener spec + design + verification completos y pasar todas las verificaciones antes de considerar el proyecto Beta.
