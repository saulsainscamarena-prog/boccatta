# Ajuste de Memoria y Saneamiento Técnico

Spec ID: `ajuste-memoria-y-saneamiento`
Capability: `general`
Fecha: `2026-06-04`

## Problema

1. Los recursos virtuales restringidos bloquean la compilación debido a asignación máxima excesiva de heap en los daemons.
2. La deuda técnica de folios legacy en SharedPreferences ensucia la API interna del gestor de sincronización offline.

## Objetivos

- Reducir los requisitos de memoria del demonio de Gradle y el compilador de Kotlin en un 50% para acomodarse a las capacidades del sandbox.
- Limpiar el código deprecado inactivo en `OfflineManager.kt`.

## No Objetivos

- Rediseñar el almacenamiento persistente activo de folios o transacciones.

## Requisitos Funcionales

- **R1**: Ajustar límites de heap y metaspace de Gradle daemon en `gradle.properties` a 2GB y 512MB respectivamente.
- **R2**: Ajustar límites de heap y metaspace de Kotlin daemon en `gradle.properties` a 1GB y 256MB respectivamente.
- **R3**: Eliminar funciones de ticket Shared Preferences obsoletas de `OfflineManager.kt`.

## Requisitos No Funcionales

- **Rendimiento de compilación**: La reducción de memoria no debe degradar excesivamente los tiempos de compilación incremental local.
- **Persistencia/offline**: Las transacciones de checkout offline deben seguir operando mediante SQLite de forma atómica.

## Criterios de Aceptación

- [ ] Gradle y Kotlin compilan sin OOM / CLR error 1455.
- [ ] No quedan llamadas a SharedPreferences de folios legacy.
- [ ] El script de verificación de Mojibake corre limpio.
