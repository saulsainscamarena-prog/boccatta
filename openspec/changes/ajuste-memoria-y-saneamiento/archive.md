# Archive: ajuste memoria y saneamiento

Spec ID: `ajuste-memoria-y-saneamiento`
Capability: `general`
Fecha: `2026-06-04`

## Resultado Final

- Se ajustó con éxito la configuración de JVM heap en `gradle.properties` de 4GB/2GB a 2GB/1GB, mitigando por completo los bloqueos OOM (error 1455) y la inicialización del CLR de PowerShell en el sandbox.
- Se eliminaron quirúrgicamente todas las funciones obsoletas de Shared Preferences para folios en `OfflineManager.kt`.

## Cambios Entregados

- **[gradle.properties](file:///c:/Users/ia/AndroidStudioProjects/bocatta/gradle.properties)**: Reducción de heap y metaspace de Gradle daemon y Kotlin compilation daemon a la mitad.
- **[OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)**: Remoción de `generarCodigoTicket`, `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal` y `leerUltimoTicketLocalLegacy`. Simplificación de `guardarVentaOffline` para omitir el parámetro.

## Verificacion Ejecutada

- Compilación incremental y limpia exitosa (`BUILD SUCCESSFUL in 36s`).
- Ejecución completa de tests unitarios del módulo POS (`BUILD SUCCESSFUL in 30s` con 0 fallos).
- Análisis estructural y de Mojibake limpio en Python.

## Decisiones Preservadas

- Reducción de memoria JVM conservada para maximizar compatibilidad de sandboxes y máquinas de desarrollo livianas, manteniendo holgura para compilaciones pesadas de R8.

## Pendientes o Deuda Tecnica

- Actualizar `TECH_DEBT.md` para marcar la consolidación y limpieza de folios obsoletos como resuelta.

## Ubicacion de Archivo

Cuando el cambio este cerrado, mover esta carpeta a:

```text
openspec/changes/archive/2026-06-04-ajuste-memoria-y-saneamiento/
```
