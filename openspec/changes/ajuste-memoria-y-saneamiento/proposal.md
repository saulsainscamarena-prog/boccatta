# Proposal: Ajuste de Memoria y Saneamiento Técnico

Spec ID: `ajuste-memoria-y-saneamiento`
Capability: `general`
Strict TDD: `true`

## Problema

- Los warnings de compilación de `OfflineManager.kt` causados por métodos deprecados ensucian la traza del compilador y representan deuda técnica.
- El host de desarrollo requiere una configuración estable y performante de JVM heap para soportar el procesamiento de KSP/Room sin ralentizaciones.

## Alcance

- Mantener `org.gradle.jvmargs` en `-Xmx4096m` (y metaspace en `1024m`) en `gradle.properties` para garantizar el máximo rendimiento del daemon.
- Mantener `kotlin.daemon.jvmargs` en `-Xmx2048m` (y metaspace en `512m`) en `gradle.properties` para el compilador Kotlin.
- Eliminar de `OfflineManager.kt`: `generarCodigoTicket`, `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal` y `leerUltimoTicketLocalLegacy`.
- Ajustar `guardarVentaOffline` para omitir el parámetro `legacyUltimoTicket`.

## Fuera de Alcance

- Modificar consultas SQLite u otras implementaciones activas de folios.

## Archivos Afectados Esperados

- [gradle.properties](file:///c:/Users/ia/AndroidStudioProjects/bocatta/gradle.properties)
- [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)

## Riesgos

- **Ventas**: Ninguno. El checkout local utiliza SQLite.
- **Inventario**: Ninguno.
- **Offline/sync**: Ninguno.
- **Caja/pagos**: Ninguno.
- **UI**: Ninguno.
- **Seguridad**: Ninguno.

## Research Externo

- Web requerida: no

## Estrategia de Rollback

- Revertir los cambios locales mediante git:
  `git checkout -- gradle.properties app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt`

## Criterios de Exito

- [ ] Gradle y compilador Kotlin instancian procesos sin fallos de reserva de memoria JRE (Error 1455).
- [ ] `OfflineManager.kt` queda libre de funciones obsoletas.
- [ ] La validación de Mojibake resulta limpia.

## Checkpoint Humano

Aprobado por el usuario ("incia con este plan").
