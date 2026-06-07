# Research: SalesScreen Compose Performance Audit

Spec ID: `perf-compose`
Capability: `ui-performance`
Fecha: `2026-06-06`

## Brief Humano

Auditar `SalesScreen.kt` y los grids (como `SalesCatalogSection` y `SalesCartSection`) por problemas de recomposición ("jank") y asegurar el uso correcto de `derivedStateOf`.

## Web Research Gate

- Research question: ¿Cuáles son las mejores prácticas para optimizar `derivedStateOf`, Immutable Collections, y Lazy Grids en Jetpack Compose?
- Source category from `openspec/source-catalog.yaml`: `android`
- Why local context is insufficient: El contexto local y las skills (`android-compose-performance`) son suficientes.
- Expected decision: Aplicar reglas de inmutabilidad y evitar recomposiciones globales.
- Stop condition: Identificados los cuellos de botella exactos en `SalesScreen`.
- Budget: none

## Contexto Local Leído

- Constitution: Sí
- AGENTS: Sí
- Skill registry: `android-compose-performance`
- Archivos inspeccionados:
  - `SalesScreen.kt`
  - `SalesViewModelV2.kt`
  - `SalesScreenSections.kt`
  - `CarritoPanelV2.kt`
  - `ModelsV2.kt`

## Alternativas

### Opción A (Recomendada)
- Descripción: 
  1. Extraer la lectura de `ultimaInteraccion` fuera de la firma del `LaunchedEffect` en `SalesScreen` para evitar que cada *touch* recomponga la pantalla entera.
  2. Anotar clases como `SalesInventoryProductV2` e `ItemCarritoV2` con `@Immutable`.
  3. Evitar que `alertasStockEstable` instancie un nuevo Map (`.toMap()`) en cada cambio, y en su lugar usar un wrapper estable o una colección inmutable real.
  4. Agregar `kotlinx-collections-immutable` al proyecto para usar `ImmutableList` / `ImmutableMap` y lograr que el catálogo de productos y el carrito de compras puedan skipear la recomposición.
- Ventajas: Resuelve de raíz el jank sin refactorizar toda la arquitectura.
- Costos: Añadir dependencia oficial de Kotlin para colecciones inmutables.
- Riesgos: Prácticamente nulos.

### Opción B
- Descripción: Crear clases de envoltorio (Wrappers) `@Immutable` propios para las listas (ej. `StableList<T>(val items: List<T>)`) y usar lambdas para lecturas diferidas de estado en lugar de pasar colecciones enteras.
- Ventajas: No añade dependencias.
- Costos: Más boilerplate y código repetitivo en la UI.
- Riesgos: Es propenso a errores humanos si un desarrollador extrae la lista del wrapper dentro del bloque incorrecto.

## Decision Recomendada

Opción A. Utilizar `kotlinx.collections.immutable` (agregándolo a `libs.versions.toml`) para que Jetpack Compose infiera correctamente la estabilidad del estado en `SalesScreen` y agregar `@Immutable` a los modelos del dominio de UI.

## Suposiciones Externas

N/A

## Tradeoffs

Pequeño incremento en la curva de aprendizaje al usar `ImmutableList` en vez de `List` normal, pero los beneficios en performance de mostrador (que es crítico) lo superan con creces.
