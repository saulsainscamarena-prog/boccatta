# Design: Ajuste de Memoria y Saneamiento Técnico

Spec ID: `ajuste-memoria-y-saneamiento`

## Resumen Técnico

- Reducción del tamaño de heap requerido para la compilación del proyecto en `gradle.properties`.
- Retiro definitivo del código obsoleto de SharedPreferences en `OfflineManager.kt`.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio técnico | Prueba |
|-----------|--------------|----------------|--------|
| R1 (Memoria Gradle) | `gradle.properties` | Modificar `org.gradle.jvmargs` a `-Xmx2048m -XX:MaxMetaspaceSize=512m` | Inicio exitoso de daemon de Gradle sin OOM 1455 |
| R2 (Memoria Kotlin) | `gradle.properties` | Modificar `kotlin.daemon.jvmargs` a `-Xmx1024m -XX:MaxMetaspaceSize=256m` | Compilación sin fallas de memoria heap del compilador |
| R3 (Remover obsoletos) | `OfflineManager.kt` | Eliminar `generarCodigoTicket`, `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal` y helper privado | Compilación y revisión sintáctica limpia |

## APIs, Contratos y Datos

- No se alteran APIs de red ni de base de datos activa.
- Se retira del módulo de sync la dependencia indirecta del SharedPreferences de tickets antiguos.
