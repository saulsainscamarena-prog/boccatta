# Proposal: SalesScreen Compose Performance Audit

Spec ID: `perf-compose`
Capability: `ui-performance`
Strict TDD: `no`

## Problema

1. **Recomposición Global Innecesaria**: En `SalesScreen.kt`, el estado `ultimaInteraccion` (que se actualiza con cada touch o key event) está siendo leído como Key de un `LaunchedEffect`. Esto causa que **la pantalla entera se recomponga en cada toque**.
2. **Listas y Mapas Inestables (`List` / `Map`)**: Compose infiere `List` y `Map` como inestables. `SalesCatalogSection` recibe `filteredProducts: List` y `CarritoPanelV2` recibe `carrito: List`. Cada vez que el padre se recompone (por el punto 1 o por cualquier otro cambio de estado), estos componentes se recomponen completos.
3. **Pérdida de Derivación Eficiente**: En `alertasStockEstable`, el código `derivedStateOf { vmV2.alertasStock.toMap() }` crea un nuevo `Map` cada vez que el stock de *un* insumo cambia. Al pasarlo a la grilla, se invalida el catálogo entero, forzando recomposición de todos los productos en vez de solo aquel cuyo stock cambió.
4. **Modelos Inestables**: `SalesInventoryProductV2` y `ItemCarritoV2` no están anotados con `@Immutable`, lo que impide a Compose saltar su evaluación en `items()`.

## Alcance

- Corregir el `LaunchedEffect` del temporizador de inactividad en `SalesScreen`.
- Añadir la dependencia `kotlinx-collections-immutable` a `libs.versions.toml` y sincronizar.
- Convertir estados derivados clave (`alertasStockEstable`, `filteredProducts`) para usar `ImmutableMap` e `ImmutableList`.
- Modificar componentes de UI (`SalesCatalogSection`, `CarritoPanelV2`) para aceptar `ImmutableList` e `ImmutableMap`.
- Anotar clases DTO (`SalesInventoryProductV2`, `ItemCarritoV2`, `ClienteV2`) con `@Immutable`.

## Fuera de Alcance

- Modificar la lógica de negocio de carrito, caja, inventario o descuentos.
- Rediseño visual de componentes.

## Archivos Afectados Esperados

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/java/com/bocatta/pos/domain/model/ModelsV2.kt`
- `app/src/main/java/com/bocatta/pos/domain/model/SalesInventoryProductV2.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/ventas/SalesScreen.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/ventas/SalesScreenSections.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/CarritoPanelV2.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/DialogosVentas.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/ProductCardPremium.kt`

## Riesgos

- Ventas: Ninguno funcional.
- Inventario: Ninguno.
- Offline/sync: Ninguno.
- Caja/pagos: Ninguno.
- UI: Beneficio alto. Reduce uso de CPU en dispositivos tablet de bajas prestaciones.
- Seguridad: Ninguno.

## Estrategia de Rollback

- Revertir los commits.

## Criterios de Exito

- [ ] `SalesScreen` no recompone catálogo entero al tocar la pantalla.
- [ ] `SalesCatalogSection` usa `ImmutableList` y `ImmutableMap` volviéndose skippable.
- [ ] Agregar un producto al carrito no recompone todos los ítems anteriores.

## Checkpoint Humano

No pasar a implementar hasta que esta propuesta esté revisada.
