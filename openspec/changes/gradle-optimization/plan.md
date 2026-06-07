# Plan Tecnico: Gradle Optimization Audit

Spec ID: `gradle-optimization`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`

## Punto de Entrada Real

Describe donde vive hoy el comportamiento y por que ese es el owner correcto.

- Build System: Los scripts de configuracion en Kotlin DSL (`build.gradle.kts`, `app/build.gradle.kts`) y el catalogo TOML (`libs.versions.toml`).

## Archivos Probables

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`

## Estrategia

1. **Ajuste del TOML**:
   - Agregar las versiones y plugins para Detekt, Ktlint y Jacoco en `libs.versions.toml`.
   - Modificar la version de KSP si esta usando un esquema erroneo (`2.3.7`) y no alineado con `2.3.21-1.0.x`.
2. **Refactor de Scripts de Gradle**:
   - Reemplazar los ids en duro en los bloques `plugins { }` de `build.gradle.kts` y `app/build.gradle.kts` con alias del catalogo.
3. **Metricas de Compose**:
   - En `app/build.gradle.kts`, agregar el bloque `composeCompiler` y utilizar project properties para habilitarlo unicamente bajo demanda:
     ```kotlin
     composeCompiler {
         if (project.hasProperty("enableComposeCompilerReports")) {
             metricsDestination = layout.buildDirectory.dir("compose_metrics")
             reportsDestination = layout.buildDirectory.dir("compose_metrics")
         }
     }
     ```

## Contratos a Revisar

- Modelos: N/A
- Interfaces: N/A
- Koin: N/A
- Firestore/Room: Asegurarse de que KSP siga compilando exitosamente las entidades de Room despues del cambio de versionamiento (si aplica).
- WorkManager/sync: N/A

## Impacto Offline

- Venta online: N/A
- Venta offline: N/A
- Reconexion: N/A
- Deduccion de inventario: N/A
- Idempotencia/reintentos: N/A

## Impacto UI

- Movil: N/A
- Tablet: N/A
- Estados de carga/error: N/A
- Eventos consumibles: N/A
- Accesibilidad: N/A

## Plan de Pruebas

- Gradle:
  - Ejecutar `.\gradlew.bat compileDebugKotlin` despues del cambio de dependencias.
  - Ejecutar `.\gradlew.bat assembleDebug -PenableComposeCompilerReports=true` y verificar la existencia de archivos de diagnostico en el output del build.
  - Ejecutar la tarea de lint/check (`.\gradlew.bat detekt`) para asegurar que el plugin portado sigue activo.

## Criterio para No Continuar

Enumera condiciones que deben detener implementacion hasta aclarar alcance o datos.

- No se debe aplicar codigo si la compilacion inicial con `compileDebugKotlin` falla antes de hacer modificaciones, lo que sugeriria un problema en el entorno local (e.g. daemons de Gradle bloqueados en Windows).
