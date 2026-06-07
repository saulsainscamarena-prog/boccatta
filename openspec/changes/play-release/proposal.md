# Proposal: Play Distribution Readiness Audit

Spec ID: `play-release`
Capability: `Play Store Distribution`
Strict TDD: `no`

## Problema

- Bocatta POS necesita publicarse en Google Play.
- Actualmente la compilación `release` está preconfigurada para R8 (`minifyEnabled = true`, `shrinkResources = true`), y quita los flags de DEMO, pero:
  1. Carece de bloque `signingConfigs` para generar un App Bundle (.aab) firmado automáticamente en modo release.
  2. `versionCode` y `versionName` están "hardcodeados" en el script de Gradle en valores iniciales (1 y "1.0").
  3. Faltaría una configuración explícita (aunque es opcional, es buena práctica) para asegurarse de empaquetar como App Bundle para subir a Play Store.

## Alcance

- Añadir un bloque de configuración de firmado (`signingConfigs { release { ... } }`) en `app/build.gradle.kts` que lea de `local.properties` de forma segura.
- Establecer estrategia de variables para incremento de versión (o documentar su flujo manual).
- Validar y optimizar configuración de ProGuard/R8 para dependencias críticas (Firebase, Room, Serialization ya parecen estar cubiertas, pero se hará verificación final).

## Fuera de Alcance

- Crear la cuenta de Google Play Developer.
- Crear el keystore local real (el agente no generará archivos binarios `.jks`, sólo preparará el archivo Gradle y las instrucciones para el desarrollador).
- Configurar automatizaciones (CI/CD GitHub Actions).

## Archivos Afectados Esperados

- `app/build.gradle.kts`
- `local.properties` (añadir plantillas/ejemplos de keys, asegurando que el archivo ya esté en `.gitignore`).

## Riesgos

- Ventas: N/A.
- Inventario: N/A.
- Offline/sync: N/A.
- Caja/pagos: N/A.
- UI: N/A.
- Seguridad: Riesgo de commitear credenciales del keystore por accidente. Se mitigará utilizando `local.properties` que está en `.gitignore`.

## Research Externo

- Web requerida: no
- Pregunta investigada: Configuración de firmado de release local en Gradle.
- Fuentes primarias: Android Developer Docs.
- Decision tomada: Usar `local.properties` para alojar variables como `keystore.path`, `keystore.password`, `key.alias`, y `key.password`.
- Suposicion sensible a version: AGP 9 requiere usar sintaxis `Properties()` y `file()` de la misma forma que ya se hace para la configuración de `DEMO_EMAIL`.

## Estrategia de Rollback

- Revertir los cambios en `app/build.gradle.kts`. No afecta la compilación en modo debug que usan los desarrolladores diariamente.

## Criterios de Exito

- [ ] `app/build.gradle.kts` tiene un `signingConfig` funcional para `release` (condicional si el archivo keystore existe).
- [ ] Ejecutar `.\gradlew.bat bundleRelease` no rompe el build (si se provee el keystore).
- [ ] Se establece una estrategia documentada para actualizar la versión de la app.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
