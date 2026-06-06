# Plan Técnico: Saneamiento de Offline Manager

Spec ID: `saneamiento-offline-manager`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)

## Punto de Entrada Real

- Repositorio/data: `OfflineManager.kt` es un singleton (`object`) local del POS encargado de encolar las ventas y operaciones offline en la base de datos local SQLite.

## Archivos Probables

- [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)

## Estrategia

1. Abrir `OfflineManager.kt`.
2. Remover las declaraciones de Shared Preferences obsoletas para folios de ticket.
3. Asegurar que las importaciones no utilizadas sean removidas (como `android.content.Context` si ya no es necesario en los métodos eliminados, aunque sigue siendo necesario en `isNetworkAvailable(context)`, `guardarVentaOffline(context)`, etc.).
4. Ejecutar el script Python de verificación de Mojibake.
5. Ejecutar la compilación unitaria de Gradle.

## Contratos a Revisar

- Modelos: Sin cambios.
- Interfaces: Sin cambios.
- Koin: Sin cambios.
- Firestore/Room: Sin cambios.
- WorkManager/sync: Sin cambios.

## Impacto Offline

- Venta online: Sin cambios.
- Venta offline: Sin cambios.
- Reconexión: Sin cambios.
- Deducción de inventario: Sin cambios.
- Idempotencia/reintentos: Sin cambios.

## Impacto UI

- N/A.

## Plan de Pruebas

- Gradle: `./gradlew.bat compileDebugKotlin` para verificar que la eliminación no rompa ninguna referencia en la compilación del módulo Kotlin.
- Unit: `./gradlew.bat testDebugUnitTest` para re-ejecutar todos los contratos unitarios y validar 0 fallos.

## Criterio para No Continuar

- Si se detecta alguna dependencia de código legacy no mapeada en nuestra búsqueda que cause fallos de compilación tras la remoción.
