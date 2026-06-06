# Tasks: Ajuste de Memoria y Saneamiento Técnico

Spec ID: `ajuste-memoria-y-saneamiento`

## Tareas

- [x] 1. Ajustar los valores de asignación de memoria máxima de JVM en `gradle.properties`.
  - Archivos: `gradle.properties`
  - Verificación: Revisión visual.

- [x] 2. Detener daemons de Gradle anteriores para liberar memoria del sistema.
  - Comando: `cmd.exe /c ".\gradlew.bat --stop"`
  - Verificación: Estado de daemons libre.

- [x] 3. Eliminar `@Deprecated fun generarCodigoTicket` en `OfflineManager.kt`.
  - Archivos: `OfflineManager.kt`
  - Verificación: Revisión visual.

- [x] 4. Eliminar `@Deprecated fun obtenerUltimoTicketLocal`, `@Deprecated fun guardarUltimoTicketLocal` y `leerUltimoTicketLocalLegacy` en `OfflineManager.kt`.
  - Archivos: `OfflineManager.kt`
  - Verificación: Revisión visual.

- [x] 5. Ajustar `guardarVentaOffline` para omitir `legacyUltimoTicket`.
  - Archivos: `OfflineManager.kt`
  - Verificación: Revisión visual.

- [x] 6. Ejecutar compilación y pruebas unitarias de Gradle con los nuevos límites de memoria reducidos.
  - Comando: `cmd.exe /c ".\gradlew.bat compileDebugKotlin" && cmd.exe /c ".\gradlew.bat testDebugUnitTest"`
  - Resultado: BUILD SUCCESSFUL (compilación y unit tests pasando sin errores en 36s/30s).

- [x] 7. Ejecutar verificación de Mojibake.
  - Comando: `python C:\Users\ia\.gemini\antigravity\brain\3b8affe2-da2c-415e-b85f-74740386cf35\scratch\audit_runner.py`
  - Resultado: Aprobado (Preflight OK y sin mojibake).
