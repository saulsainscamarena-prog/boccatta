# Plan Tecnico: a11y-compose

Spec ID: `a11y-compose`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- `BocattaComponents.kt`

## Punto de Entrada Real

- UI: `BocattaComponents.kt` contiene la biblioteca de componentes base de Jetpack Compose. Modificaremos los Modifier que construyen estos componentes.

## Archivos Probables

- `app/src/main/java/com/bocatta/pos/presentation/ui/components/BocattaComponents.kt`

## Estrategia

1. Modificar `BocattaMetricCard`: Agregar `Modifier.semantics(mergeDescendants = true) {}` a su layout principal.
2. Modificar `BocattaButton`: Añadir `Modifier.semantics { if (cargando) stateDescription = "Cargando" }` para exponer explícitamente a TalkBack cuando está procesando.
3. Modificar `BocattaSectionTitle`: Agregar `Modifier.semantics { heading() }` al Row o al Text.
4. Modificar `BocattaFilaResumen`: Agregar `Modifier.semantics(mergeDescendants = true) {}` al Row que agrupa la etiqueta y el valor.
5. Modificar `BocattaCartItemRow`: Evaluar el uso de `clearAndSetSemantics` o `mergeDescendants` en la información textual, dejando intacto el `IconButton` de borrar para que mantenga un touch target adecuado.

## Contratos a Revisar

- Modelos: N/A
- Interfaces: N/A
- Koin: N/A
- Firestore/Room: N/A
- WorkManager/sync: N/A

## Impacto Offline

- Venta online: Sin impacto.
- Venta offline: Sin impacto.
- Reconexion: Sin impacto.
- Deduccion de inventario: Sin impacto.
- Idempotencia/reintentos: Sin impacto.

## Impacto UI

- Movil: Sin impacto visual, solo lector de pantalla.
- Tablet: Sin impacto visual, solo lector de pantalla.
- Estados de carga/error: TalkBack narrará "Cargando" en los botones primarios.
- Eventos consumibles: N/A
- Accesibilidad: Las métricas, resúmenes y títulos serán navegables lógicamente por TalkBack.

## Plan de Pruebas

- Unit: N/A
- Instrumented: N/A
- Compose UI: N/A
- Maestro/manual: Compilación con `./gradlew.bat compileDebugKotlin`.
- Gradle: Compilación local para validar la sintaxis.

## Criterio para No Continuar

- Si alguna modificación rompe el rendering visual de un componente.
