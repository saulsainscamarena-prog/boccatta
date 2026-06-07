# Proposal: Dead Code & Redundant Configurations Audit

Spec ID: `dead-code-001`
Capability: `Technical Debt / Code Quality`
Strict TDD: `no`

## Problema

- A medida que el proyecto Bocatta POS crece, se acumula código muerto (dead code), recursos XML huérfanos (unused resources) y configuraciones redundantes en Proguard/R8 que aumentan el tamaño del APK y los tiempos de compilación.

## Alcance

- Auditar clases, funciones y variables no utilizadas usando Android Lint.
- Auditar recursos XML (layouts, drawables, strings) no utilizados.
- Auditar `app/proguard-rules.pro` para identificar reglas demasiado amplias (e.g. paquetes completos) o reglas redundantes para librerías que ya proveen sus propias reglas.

## Fuera de Alcance

- Eliminación física de archivos y código en esta fase de auditoría.
- Modificación de dependencias en Gradle.
- Cambios de arquitectura o refactorización de lógica.

## Archivos Afectados Esperados

- En esta fase: Ninguno (solo reporte).
- En la fase de implementación: `app/proguard-rules.pro`, clases Kotlin no usadas, archivos en `app/src/main/res/`.

## Riesgos

- Ventas: Si se marca como "muerto" código que se invoca por reflection en el checkout (ej. serialización), eliminarlo causará crash.
- Inventario: Similar a ventas.
- Offline/sync: Modelos de Room o Workers que Lint considere no utilizados pero que son instanciados dinámicamente.
- Caja/pagos: SDKs de hardware o reflection.
- UI: Recursos construidos dinámicamente o por nombre (menos común en Compose, pero posible).
- Seguridad: Quitar reglas de R8 podría exponer código de forma imprevista o romper obfuscación.

## Research Externo

- Web requerida: no
- Pregunta investigada: N/A
- Fuentes primarias: N/A
- Decision tomada: Usar análisis estático local.
- Suposicion sensible a version: AGP soporta recolección de dead code a través de R8 full mode, pero Lint es más seguro para reportar recursos y código a nivel fuente.

## Estrategia de Rollback

- N/A para la fase de auditoría. Para la fase de limpieza, un commit atómico reversible es suficiente.

## Criterios de Exito

- [x] Documentos SDD creados y aceptados.
- [ ] Ejecución de Lint o R8 analyzer completada.
- [ ] Reporte entregado al usuario con los candidatos a eliminación.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada por el usuario.
