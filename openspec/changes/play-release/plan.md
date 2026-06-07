# Plan Tecnico: Play Distribution Readiness Audit

Spec ID: `play-release`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Archivos propietarios del flujo real antes de editar codigo.
- Skill `android-release-play-distribution`.

## Punto de Entrada Real

Describe donde vive hoy el comportamiento y por que ese es el owner correcto.

- UI: N/A
- ViewModel: N/A
- Dominio/use case: N/A
- Repositorio/data: N/A
- DI: N/A
- Tests: N/A
- Build/Config: La configuración vive en `app/build.gradle.kts` (y `gradle.properties`). Este es el único responsable de configurar cómo se empaqueta, ofusca y firma la app.

## Archivos Probables

- `app/build.gradle.kts`
- `local.properties` (para agregar las keys de ejemplo localmente)
- `.gitignore` (para verificar que `*.jks` y `local.properties` están ignorados)

## Estrategia

1. **Revisar / Modificar `.gitignore`**: Validar que `*.jks` y `*.keystore` estén excluidos para no fugar llaves de producción.
2. **Configuración de Versiones**: Extraer `versionCode` y `versionName` a variables leídas desde un archivo de configuración (ej. `version.properties`) o definir un esquema manual en la cabecera de `build.gradle.kts` que sea fácil de mantener.
3. **Firma de Release**: 
   - Añadir la lógica para cargar las credenciales del keystore desde `local.properties`.
   - Crear el bloque `signingConfigs { create("release") { ... } }` y asignarlo al `buildType` de `release`.
   - Configurar esto condicionalmente, de modo que si las credenciales no existen, el build no falle abruptamente (útil para CI o desarrolladores nuevos), o emitir un warning en consola.
4. **Validación de App Bundles**: Asegurarse de que el comando `bundleRelease` funcionaría y usar R8 de manera estricta pero segura.

## Contratos a Revisar

- Modelos: Validar que `proguard-rules.pro` cubra bien la persistencia offline, aunque ya tiene reglas para Firestore y Serialización.
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

- Movil: Sin impacto.
- Tablet: Sin impacto.
- Estados de carga/error: Sin impacto.
- Eventos consumibles: Sin impacto.
- Accesibilidad: Sin impacto.

## Plan de Pruebas

- Unit: N/A
- Instrumented: N/A
- Compose UI: N/A
- Maestro/manual: N/A
- Gradle: 
  - Validar que compile en release: `.\gradlew.bat compileReleaseKotlin`
  - Validar bundle: `.\gradlew.bat bundleRelease`
  - Validar que las keys no existan en repositorios públicos (`git status`).

## Criterio para No Continuar

Enumera condiciones que deben detener implementacion hasta aclarar alcance o datos.

- Falta de decisión sobre quién administrará el archivo de firma local (.jks) y el control de versiones automatizado. Para la prueba inicial, se asume que el usuario desarrollador lo mantendrá localmente.
