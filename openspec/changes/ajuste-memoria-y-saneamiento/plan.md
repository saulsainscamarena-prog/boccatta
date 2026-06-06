# Plan Técnico: Ajuste de Memoria y Saneamiento Técnico

Spec ID: `ajuste-memoria-y-saneamiento`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- [gradle.properties](file:///c:/Users/ia/AndroidStudioProjects/bocatta/gradle.properties)
- [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)

## Punto de Entrada Real

- Configuración compilación: `gradle.properties` en la raíz del proyecto.
- Repositorio/data: `OfflineManager.kt` para remover SharedPreferences obsoletas de folios.

## Archivos Probables

- [gradle.properties](file:///c:/Users/ia/AndroidStudioProjects/bocatta/gradle.properties)
- [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)

## Estrategia

1. Modificar `gradle.properties` reduciendo los límites de memoria heap y metaspace a la mitad (2GB/1GB).
2. Detener daemons de Gradle huérfanos ejecutando `cmd.exe /c ".\gradlew.bat --stop"`.
3. Editar `OfflineManager.kt` para remover las 4 funciones deprecadas de persistencia de folios.
4. Ajustar `guardarVentaOffline` en `OfflineManager.kt` para no hacer referencia al contador legacy.
5. Ejecutar la compilación mediante `./gradlew.bat compileDebugKotlin` para verificar que la reducción de memoria permite instanciar el compilador con éxito.
6. Correr el script de verificación de Mojibake.

## Plan de Pruebas

- Gradle: `cmd.exe /c ".\gradlew.bat compileDebugKotlin"`
- Unit: `cmd.exe /c ".\gradlew.bat testDebugUnitTest"`
