# Research: Play Distribution Readiness Audit

Spec ID: `play-release`
Capability: `Play Store Distribution`
Fecha: `2026-06-06`

## Brief Humano

Resume el pedido original, contexto del negocio y restricciones conocidas.

- Realizar una auditoría de Bocatta POS para evaluar su preparación para la distribución en Google Play.
- Revisar configuración de App Bundles, estrategia de versionado, preparación para firmado (signing) y las variantes de compilación seguras para producción.
- No modificar código, únicamente generar el SDD y reportar de vuelta.

## Web Research Gate

Completar antes de buscar en internet.

- Research question: ¿Cuáles son las mejores prácticas actuales en Android Gradle Plugin 9+ para firmar y configurar App Bundles y versionado dinámico?
- Source category from `openspec/source-catalog.yaml`: `Android Official Docs`, `Gradle Docs`.
- Why local context is insufficient: Necesitamos validar si hay nuevas configuraciones recomendadas en AGP 9 para bundles.
- Expected decision: Configuración segura de keystore y estrategia de cálculo de `versionCode`.
- Stop condition: Determinar la configuración exacta de `signingConfigs` y propiedades para `local.properties`.
- Budget: none

Si `Budget` es `none`, explicar por qué el repo local basta:
Las directrices de Google Play y AGP son estándar. Sabemos que necesitamos un bloque de `signingConfigs` en Gradle y que las propiedades del keystore no deben versionarse, sino cargarse de `local.properties` o variables de entorno. La estrategia de versiones suele delegarse a CI o mantenerse simple.

## Contexto Local Leído

- Constitution: `openspec/constitution.md` (asumido).
- AGENTS: Revisado (reglas sobre no modificar el entorno sin plan).
- Skill registry: `android-release-play-distribution`.
- Source catalog: N/A.
- Archivos inspeccionados:
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
  - `app/proguard-rules.pro`
  - `gradle.properties`

## Fuentes Externas Consultadas

Ninguna requerida directamente, se utiliza el conocimiento de Android App Bundles, AGP 9 y distribución de Play Console.

## Alternativas

### Opcion A: Keystore local referenciado en `local.properties` (Recomendada)

- Descripcion: Crear un bloque `signingConfigs` para `release` que lea las contraseñas y alias del archivo `local.properties` (que no se sube a Git).
- Ventajas: Sencillo, seguro localmente, fácil de entender para desarrolladores individuales.
- Costos: Requiere configuración manual inicial del desarrollador.
- Riesgos: Perder el keystore local (debe respaldarse independientemente o usar Play App Signing).

### Opcion B: Firmado vía CI/CD (GitHub Actions)

- Descripcion: No configurar `signingConfigs` en el proyecto, delegar el firmado de release a un flujo de CI/CD que inyecte el keystore y variables.
- Ventajas: Keystore nunca toca el entorno local de desarrollo, escalable para equipos.
- Costos: Requiere configurar pipeline CI/CD (ej. GitHub Actions).
- Riesgos: Mayor complejidad inicial.

## Decision Recomendada

- Utilizar **Opción A**, leyendo variables de entorno si existen, con *fallback* a `local.properties` para soportar tanto builds locales seguros como CI/CD en el futuro.
- Implementar **Play App Signing** en Play Console, usando la llave local solo como "Upload Key".

## Suposiciones Externas

- El usuario creará el keystore de subida (Upload Key) por su cuenta a través de Android Studio.
- Se tiene o se creará una cuenta de Google Play Console.

## Tradeoffs

- Manejo de versión manual vs autogenerada: Para comenzar, un versionado manual administrado es preferible a scripts complejos de Git tag, a menos que el proyecto ya use CI.

## Preguntas Que Deben Aclararse Antes de Proponer

- ¿Desea el usuario delegar el incremento de versión a un CI/CD, o prefiere actualizar `versionCode` y `versionName` manualmente en Gradle antes de cada release?
