# Proposal: Multimodule Migration

Spec ID: `multimodule-migration`
Capability: `architecture`
Strict TDD: `false`

## Problema

- El proyecto Bocatta POS creció como un monolito de un solo módulo (`:app`), lo que provoca tiempos de compilación lentos, falta de encapsulamiento estricto, acoplamiento de responsabilidades (dominio, red, ui) y dificultad para escalar en un entorno de desarrollo concurrente o pruebas aisladas.
- La ausencia de límites de módulos permite dependencias circulares accidentales o filtraciones de la capa de datos hacia la UI.

## Alcance

- Migrar el código monolítico a una Arquitectura Híbrida Basada en Features (basada en *Now in Android*).
- Fase 0: Estabilización del código base (asegurar que no haya errores de compilación ni tests rotos actuales).
- Fase 1: Build Logic - Implementación de Convenciones de Gradle (*Convention Plugins*) en un módulo `build-logic` para estandarizar la configuración de builds en todos los módulos.
- Fase 2: Core modules - Extracción de funcionalidades y recursos compartidos en `:core:model`, `:core:database`, `:core:network` y `:core:ui`.
- Fase 3: Feature modules - Extracción de características funcionales de negocio en `:feature:auth`, `:feature:admin`, `:feature:ventas` y `:feature:inventario`.
- Fase 4: Final assembly - Ensamblado final de la aplicación en el módulo `:app` integrando la Inyección de Dependencias con Koin y la navegación mediante `AppNavGraph`.

## Fuera de Alcance

- Refactorización de la lógica de negocio profunda de ventas, inventario u offline (las funcionalidades mantienen su comportamiento exacto, solo cambian de módulo).
- Cambios drásticos en el diseño UI/UX o cambio de framework de UI.
- Porting a Kotlin Multiplatform (KMP) o Compose Multiplatform de inmediato; la migración sirve de base, pero KMP no forma parte de este sprint.

## Archivos Afectados Esperados

- `settings.gradle.kts` (creación e inclusión de múltiples subproyectos/módulos).
- `build.gradle.kts` (raíz, app, y nuevos módulos).
- Creación del directorio `build-logic` y convenciones (Kotlin DSL).
- Movimiento y refactorización masiva de paquetes desde `app/src/main/java/com/saul...` hacia los nuevos módulos `core` y `feature`.
- `AppNavGraph.kt` y configuración de módulos Koin (DI).

## Riesgos

- **Ventas**: Regresiones silenciosas por falta de dependencias correctas en DI (Koin) al desvincular `:app` de implementaciones concretas.
- **Inventario**: Problemas de resolución de clases offline (Room/SyncWorker) si `:core:database` y `:feature:inventario` no se inyectan correctamente.
- **Offline/sync**: Pérdida o fallo en el registro de `SyncWorker` y sincronización en segundo plano al cambiar rutas de paquetes.
- **Caja/pagos**: Ruptura de dependencias entre módulos si intentan conocer detalles unos de otros en lugar de usar contratos/interfaces.
- **UI**: Fallos de renderizado (recomposiciones rotas o fallos de Theming) si `:core:ui` no exporta correctamente dependencias de Material 3 y tokens.
- **Seguridad**: Exposición de lógica o secretos que deberían ser `internal` en sus respectivos módulos.

## Research Externo

- Web requerida: no
- Pregunta investigada: Estructura estándar de modularización de Google (Now in Android).
- Fuentes primarias: Patrones de modularización oficiales de Android y Gradle Convention Plugins.
- Decision tomada: Módulos organizados por tipo (`core`, `feature`) y reglas centralizadas vía `build-logic`. Las dependencias funcionales convergen solo en `app`.
- Suposicion sensible a version: Gradle 8+ y AGP 8+ soportan Version Catalogs y Build Logic de manera robusta, los cuales ya están presentes en el entorno.

## Estrategia de Rollback

- Dado el tamaño de la migración, la principal estrategia es realizar commits funcionales en cada etapa (Fases 1 a 4).
- Si la migración falla gravemente en una fase, se revierten los commits a la fase funcional anterior o directamente al commit de estabilización de la Fase 0 (monolito funcionando).
- Apoyarse fuertemente en el sistema de Control de Versiones (git).

## Criterios de Exito

- [ ] La aplicación compila correctamente después de cada fase con la nueva estructura de módulos (Build Successful).
- [ ] La estructura de directorios refleja las carpetas `core` y `feature`.
- [ ] El módulo `app` actúa puramente como pegamento (glue-code) para la inyección (Koin) y la navegación, sin contener reglas de negocio.
- [ ] Tiempos de configuración de compilación mantenidos o mejorados (gracias a Gradle Configuration Cache y Build Logic).
- [ ] Ausencia de dependencias circulares.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
