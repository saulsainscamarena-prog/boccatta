# Research: Gradle Optimization Audit

Spec ID: `gradle-optimization`
Capability: `build`
Fecha: `2026-06-06`

## Brief Humano

Resume el pedido original, contexto del negocio y restricciones conocidas.

- El usuario solicito auditar la configuracion de construccion de Gradle del proyecto Bocatta POS para encontrar oportunidades de optimizacion.
- Los elementos especificos a revisar son: uso de wrapper, catalogos de versiones, KSP, metricas del compilador Compose y R8/minify.
- No se deben realizar cambios de codigo en esta etapa, solo investigación, propuesta y plan.

## Web Research Gate

Completar antes de buscar en internet.

- Research question: Mejores practicas para KSP con Kotlin 2.3+ y habilitacion de metricas de Compose Compiler.
- Source category from `openspec/source-catalog.yaml`: build
- Why local context is insufficient: N/A, el contexto local provee los archivos a revisar, el conocimiento interno del asistente sobre el stack Android moderno es suficiente.
- Expected decision: Recomendaciones para limpiar la configuracion e incorporar metricas para optimizacion de Compose.
- Stop condition: Cuando se hayan inspeccionado todos los items solicitados en el request del usuario.
- Budget: none

Si `Budget` es `none`, explicar por que el repo local basta.
- Las reglas de optimizacion para AGP 9.2, Gradle 9.5 y el plugin `libs.versions.toml` son estandares bien conocidos y observables directamente en los archivos.

## Contexto Local Leido

- Constitution: `openspec/constitution.md` (no disponible o leido el `AGENTS.md`)
- AGENTS: Leido el `AGENTS.md` (Reglas de Trabajo para Bocatta POS).
- Skill registry: `android-gradle-build-optimization`
- Source catalog: N/A
- Archivos inspeccionados: 
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradle/libs.versions.toml`
  - `build.gradle.kts`
  - `app/build.gradle.kts`
  - `gradle.properties`
  - `gradle/gradle-daemon-jvm.properties`
  - `app/proguard-rules.pro`

## Hallazgos de Auditoria

1. **Wrapper Usage**:
   - Gradle 9.5.1 esta en uso, lo cual es correcto y moderno para AGP 9.2.1.
   - `gradle-daemon-jvm.properties` usa toolchainVersion=17.
   - `app/build.gradle.kts` usa Java 17 para source y target compatibility, y `jvmToolchain(17)` para Kotlin. El wrapper y la JVM estan perfectamente alineados.
2. **Version Catalogs**:
   - `libs.versions.toml` esta implementado para la gran mayoria de dependencias y plugins.
   - *Oportunidad*: En `build.gradle.kts` root, los plugins `ktlint` (11.6.1) y en `app/build.gradle.kts` `detekt` (1.23.6) y `jacoco` estan hardcodeados. Podrian moverse al catalogo de versiones para mayor limpieza.
3. **KSP**:
   - Usa la version `2.3.7` en el catalogo. Kotlin esta en la `2.3.21`. Normalmente KSP sigue un formato como `2.3.21-1.0.XX`.
   - *Oportunidad*: Verificar la compatibilidad exacta entre Kotlin 2.3.21 y la versión actual de KSP, y actualizar en caso de ser necesario a una nomenclatura compatible y documentada por el equipo de Google KSP.
4. **Compose Compiler Metrics**:
   - Las metricas del compilador NO estan configuradas en el bloque `composeCompiler` en el `build.gradle.kts` de la app.
   - *Oportunidad*: Habilitar flags (`reportsDestination`, `metricsDestination`) condicionadas a un parametro de Gradle (`-PenableComposeMetrics=true`) para que puedan generarse reportes y depurar recomposiciones (e.g. estabilidad de clases, skippability).
5. **R8/Minify**:
   - Esta habilitado correctamente para release (`isMinifyEnabled = true`).
   - `gradle.properties` tiene activado el modo completo (`android.enableR8.fullMode=true`).
   - Existen reglas especificas de Proguard/R8 documentadas (`app/proguard-rules.pro`) para proteger modelos y Koin. La configuracion de R8 parece robusta.

## Alternativas

### Opcion A: Clean up completo + Metricas Opcionales

- Descripcion: Mover todos los plugins faltantes a `libs.versions.toml`, validar KSP, y agregar el bloque de `composeCompiler` condicionado por una property de Gradle.
- Ventajas: Estandarizacion del 100% en el Version Catalog. Habilita metricas sin penalizar el build cache comun.
- Costos: Requiere unas pocas lineas adicionales de configuracion en `app/build.gradle.kts`.
- Riesgos: Minimos.

### Opcion B: Solo habilitar Metricas

- Descripcion: Ignorar hardcodings de plugins y unicamente agregar los flags del Compose Compiler.
- Ventajas: Menor cambio.
- Costos: Ninguno real, pero deja deuda tecnica en el Version Catalog.
- Riesgos: La deuda tecnica de versiones de build-scripts puede empeorar.

## Decision Recomendada

- Opcion A. Es el paso correcto para alinear todo el proyecto con el Version Catalog y proveer una base de diagnostico limpia para futuros problemas de rendimiento de UI (via Compose Metrics).

## Suposiciones Externas

- KSP para Kotlin 2.3.21 requiere una version de KSP tipo `2.3.21-1.0.x`.
- Compose Compiler esta integrado directamente en Kotlin (lo cual se confirma con el uso del plugin `org.jetbrains.kotlin.plugin.compose`).

## Tradeoffs

- Habilitar las metricas siempre anularía la cache del compilador por cada build y llenaría de archivos la carpeta build. Por eso deben estar ocultas tras un parametro de Gradle.

## Preguntas Que Deben Aclararse Antes de Proponer

- N/A
