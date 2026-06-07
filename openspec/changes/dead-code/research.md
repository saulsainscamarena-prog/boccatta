# Research: Dead Code & Redundant Configurations Audit

Spec ID: `dead-code-001`
Capability: `Technical Debt / Code Quality`
Fecha: `2026-06-06`

## Brief Humano

Audit Bocatta POS for dead code, unused resources, and redundant configurations (such as overly broad R8 keep rules) without modifying any code. We must follow the SDD workflow and prepare the research, proposal, and plan documents.

## Web Research Gate

- Research question: Best practices for identifying dead code in Android projects using Android Gradle Plugin 9+ and R8.
- Source category from `openspec/source-catalog.yaml`: Official Android Documentation.
- Why local context is insufficient: We need to confirm if AGP 9.0 optimizations cover redundant R8 rules automatically.
- Expected decision: Determine whether to rely solely on Android Lint or a combination of Lint and R8 Analyzer.
- Stop condition: Find the official recommendation for finding unused classes/resources in modern Android.
- Budget: none

Si `Budget` es `none`, explicar por que el repo local basta.
El repositorio local basta porque herramientas como Android Lint (`./gradlew lintDebug`) y el análisis estático en Android Studio son estándares de facto. Además, tenemos la skill local `android-r8-analyzer` y acceso a `app/proguard-rules.pro`.

## Contexto Local Leido

- Constitution: `openspec/constitution.md` (Prioridad en protección de operacion y ventas offline; cambios pequeños y verificables).
- AGENTS: `AGENTS.md` (Obligatorio SDD, reglas sobre Windows daemons).
- Skill registry: `android-r8-analyzer`.
- Archivos inspeccionados: `app/proguard-rules.pro` (Contiene reglas genéricas para modelos y serialización que podrían ser muy amplias).

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|
| N/A | N/A | N/A | N/A | Utilizaremos herramientas locales. |

## Alternativas

### Opcion A: Análisis Local Completo (Lint + R8 Analyzer)

- Descripcion: Ejecutar Android Lint para identificar recursos y clases no usadas (`UnusedResources`, `UnusedDeclaration`). Revisar manualmente `proguard-rules.pro` contra las referencias de R8.
- Ventajas: Cobertura completa de recursos y código.
- Costos: Requiere ejecución de Gradle que puede fallar por problemas de Daemons en Windows.
- Riesgos: Falsos positivos en clases usadas via reflection.

### Opcion B: Sólo Análisis de Proguard

- Descripcion: Aplicar la skill `android-r8-analyzer` unicamente sobre `proguard-rules.pro`.
- Ventajas: Muy rápido, no requiere Gradle.
- Costos: No audita clases ni recursos huérfanos.
- Riesgos: Análisis incompleto según el brief humano.

## Decision Recomendada

- Opcion A. Ejecutar Lint (`./gradlew lintDebug`) y extraer resultados, complementado con un análisis manual de `proguard-rules.pro`.

## Suposiciones Externas

- El proyecto puede compilar exitosamente sin dependencias conflictivas de Daemons una vez se limpie el entorno (o ejecutando `--no-daemon`).

## Tradeoffs

- El análisis estático de código muerto puede marcar clases de inyección de dependencias (Koin) o serialización si no están debidamente anotadas o referenciadas directamente.

## Preguntas Que Deben Aclararse Antes de Proponer

- ¿Deseamos ejecutar la eliminación automáticamente después, o sólo presentar el reporte detallado?
