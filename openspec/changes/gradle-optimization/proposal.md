# Proposal: Gradle Optimization Audit

Spec ID: `gradle-optimization`
Capability: `build`
Strict TDD: `false`

## Problema

- Aunque el proyecto usa un catalogo de versiones (`libs.versions.toml`), todavia existen configuraciones y versiones de plugins hardcodeadas en los scripts raiz y de aplicacion (`ktlint`, `detekt`, `jacoco`).
- Las metricas de Compose Compiler no estan habilitadas, lo cual limita la observabilidad sobre la estabilidad de los estados y la "skippability" de los Composables de la app (un factor critico para el rendimiento del POS).
- La version de KSP (`2.3.7`) parece no seguir el esquema estandar de versionamiento empatado a Kotlin (`2.3.21`).

## Alcance

- **Catalogos de versiones**: Mover declaraciones de plugins de `ktlint`, `detekt` y `jacoco` (y posiblemente otros) desde `build.gradle.kts` y `app/build.gradle.kts` a `libs.versions.toml`.
- **Compose Compiler**: Integrar el bloque `composeCompiler { ... }` en `app/build.gradle.kts` para exportar reportes y metricas *solo* si se pasa el flag de Gradle (`-PenableComposeCompilerReports=true`).
- **KSP**: Investigar y ajustar (si aplica) la declaracion de la version de KSP para alinearla al formato requerido por Kotlin 2.3.21 (ej. `2.3.21-1.x.x`).

## Fuera de Alcance

- Actualizaciones mayores de AGP, Kotlin, o dependencias clave del BOM de Compose.
- Modificacion de las reglas de ProGuard o R8, ya que la auditoria determino que estan bien configuradas (`fullMode` activado, perfiles adecuados).
- Modificacion de codigo productivo (Kotlin/XML).

## Archivos Afectados Esperados

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`

## Riesgos

- Ventas: N/A.
- Inventario: N/A.
- Offline/sync: N/A.
- Caja/pagos: N/A.
- UI: N/A.
- Seguridad: N/A.
- Build Cache: Habilitar las metricas incondicionalmente puede romper el cache del build y ralentizar la compilacion. Es crítico protegerlo con una condicion.

## Research Externo

- Web requerida: no
- Pregunta investigada: Estructura de versionamiento de KSP para Kotlin 2.3 y flags de Compose Metrics en Kotlin 2.0+.
- Fuentes primarias: Documentacion oficial de KSP y Jetpack Compose.
- Decision tomada: Aislar los reportes de compose detrás de un project property.
- Suposicion sensible a version: El proyecto usa Kotlin 2.3.21, el plugin de compose compiler viene de org.jetbrains.kotlin.plugin.compose, por tanto, la configuracion pertenece al bloque de Kotlin DSL `composeCompiler`.

## Estrategia de Rollback

- Revertir los cambios en los tres archivos (git checkout) ya que ninguna alteracion logica se exporta al codigo productivo.

## Criterios de Exito

- [ ] Todas las versiones de plugins residen en `libs.versions.toml`.
- [ ] Compilar el proyecto con `-PenableComposeCompilerReports=true` genera archivos `.txt` y `.csv` de metricas en la carpeta `build/compose_metrics`.
- [ ] El proyecto compila correctamente de manera normal (sin flags).

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
