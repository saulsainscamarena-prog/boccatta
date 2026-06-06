# Archive: saneamiento offline manager

Spec ID: `saneamiento-offline-manager`
Capability: `general`
Fecha: `2026-06-04`

## Resultado Final

- Limpieza exitosa de deuda técnica heredada: se eliminaron los métodos de SharedPreferences deprecados para folios de ticket en `OfflineManager.kt`.
- La configuración de memoria JVM en `gradle.properties` se mantuvo sin cambios en 4GB por decisión del usuario.

## Cambios Entregados

- **[OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)**: Remoción de las declaraciones e implementaciones obsoletas de `generarCodigoTicket`, `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal`, y `leerUltimoTicketLocalLegacy`.
- **Checkout simplificado**: En `guardarVentaOffline`, se omitió la asignación e invocación del contador legacy, apoyándose directamente en el correlativo SQLite del repositorio de datos.

## Verificacion Ejecutada

- Análisis de Mojibake y preflight estructural ejecutados de forma limpia mediante script de sanidad en Python (con salida libre de codificación corrupta).

## Decisiones Preservadas

- Mantener `org.gradle.jvmargs` y `kotlin.daemon.jvmargs` con límites de 4GB y 2GB respectivamente en `gradle.properties`, según directiva del usuario.

## Pendientes o Deuda Tecnica

- Ninguno. La deuda descrita en `TECH_DEBT.md` referente a los wrappers heredados de `OfflineManager` queda resuelta y lista para actualizarse en la siguiente consolidación general.

## Ubicacion de Archivo

Cuando el cambio este cerrado, mover esta carpeta a:

```text
openspec/changes/archive/2026-06-04-saneamiento-offline-manager/
```
