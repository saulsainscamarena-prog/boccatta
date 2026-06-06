# Research: Ajuste de Memoria y Saneamiento Técnico

Spec ID: `ajuste-memoria-y-saneamiento`
Capability: `general`
Fecha: `2026-06-04`

## Brief Humano

- El usuario indicó que la memoria máxima para el demonio de Gradle debe conservarse en 4GB (para evitar degradar el rendimiento local y no exceder los límites requeridos por KSP/Room) y se eliminan las Shared Preferences deprecadas de folios en `OfflineManager.kt`.
- Se aplicará el flujo SDD completo antes de realizar las modificaciones en el código.

## Web Research Gate

- Research question: ¿Cuáles son las implicaciones de mantener la memoria máxima asignada a Gradle daemon y Kotlin daemon en 4GB y 2GB?
- Source category: None.
- Why local context is sufficient: Es una configuración interna de rendimiento. Conservar Gradle daemon heap en 4GB y Kotlin daemon heap en 2GB es adecuado para compilaciones pesadas que requieran KSP, D8/R8 y Room, previniendo cuellos de botella locales.
- Expected decision: Mantener la memoria en 4GB/2GB.
- Budget: none

## Contexto Local Leido

- Constitution: Mantener JDK 17 y configuración limpia.
- AGENTS.md: Diagnóstico de locks y daemons atorados en Windows.

## Alternativas

### Opción A (Aprobada)
- Descripción: Conservar los límites de memoria JVM en 4GB (Gradle) y 2GB (Kotlin) en `gradle.properties` y remover código deprecado en `OfflineManager.kt`.
- Ventajas: Mantiene la compilación local al máximo rendimiento para tareas complejas de R8 y KSP.
- Costos: Riesgo de fallos OOM bajo sandboxes con restricciones extremas de paginación virtual.

### Opción B
- Descripción: Reducir los límites a 2GB y 1GB respectivamente.
- Ventajas: Mayor tolerancia en entornos de virtualización ajustados.
- Costos: Menor rendimiento del compilador en builds complejas.

## Decisión Recomendada

- Opción A: Mantener 4GB/2GB para la memoria de Gradle/Kotlin.

## Suposiciones Externas

- El sistema host de desarrollo cuenta con suficiente RAM o paginación configurada para instanciar el daemon de 4GB.

## Tradeoffs

- Se prioriza el rendimiento local de compilación incremental, asumiendo la responsabilidad del aprovisionamiento de memoria virtual en el host de desarrollo.
