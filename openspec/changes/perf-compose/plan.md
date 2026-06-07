# Plan Tecnico: SalesScreen Compose Performance Audit

Spec ID: `perf-compose`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Skill `android-compose-performance`

## Punto de Entrada Real

- **UI**: `SalesScreen.kt` (Raíz del problema de inactividad que dispara recomposiciones masivas).
- **Dominio/Data**: `ModelsV2.kt` y `SalesInventoryProductV2.kt` (Requieren anotaciones de estabilidad).
- **Gradle**: `libs.versions.toml` (Para añadir inmutabilidad a Jetpack Compose).

## Archivos Probables

1. `gradle/libs.versions.toml`
2. `app/build.gradle.kts`
3. `app/src/main/java/com/bocatta/pos/domain/model/ModelsV2.kt`
4. `app/src/main/java/com/bocatta/pos/domain/model/SalesInventoryProductV2.kt`
5. `app/src/main/java/com/bocatta/pos/presentation/ui/screens/ventas/SalesScreen.kt`
6. `app/src/main/java/com/bocatta/pos/presentation/ui/screens/ventas/SalesScreenSections.kt`
7. `app/src/main/java/com/bocatta/pos/presentation/ui/components/CarritoPanelV2.kt`

## Estrategia

1. **Gradle**: Agregar `kotlinx-collections-immutable` en el `libs.versions.toml` y sincronizar.
2. **Estabilidad de Modelos**: Anotar `SalesInventoryProductV2`, `ItemCarritoV2` y `ClienteV2` con `@Immutable`.
3. **Corrección de Eventos (SalesScreen.kt)**: 
   - Mover `ultimaInteraccion` fuera del block de la `key` de `LaunchedEffect`. Crear un loop interno `while(bloqueoPermitido)` con `delay(1000)` que compare con `ultimaInteraccion` para no forzar la recomposición del árbol completo.
4. **Adopción de Immutable Collections**:
   - `SalesScreen.kt`: En `filteredProducts`, retornar `baseProducts.filter { ... }.toImmutableList()`.
   - `SalesScreen.kt`: Cambiar `alertasStockEstable` para que regrese un `ImmutableMap` (`vmV2.alertasStock.toImmutableMap()`) u omitir la conversión manual a Map inestable.
   - `SalesScreenSections.kt`: Cambiar la firma de `SalesCatalogSection` para requerir `ImmutableList` y `ImmutableMap`.
   - `SalesScreenSections.kt`: Cambiar la firma de `SalesCartSection` y `SalesMobileCartBar` para requerir `ImmutableList<ItemCarritoV2>`.
   - `CarritoPanelV2.kt`: Requerir `ImmutableList<ItemCarritoV2>`.

## Contratos a Revisar

- **Modelos**: Modificar data classes para cumplir con `@Immutable`.
- **UI Components**: Firmas de Compose que hoy aceptan `List<T>`.

## Impacto Offline

- Venta online: N/A
- Venta offline: N/A
- Reconexion: N/A
- Deduccion de inventario: N/A
- Idempotencia/reintentos: N/A

## Impacto UI

- Movil: Interfaz reactiva fluida (60+ fps), reduciendo el jank de scrolling y escaneo de productos.
- Tablet: Evita redibujos globales de la cuadrícula, fundamental para dispositivos POS con menor memoria o CPU.

## Plan de Pruebas

- **Compose UI**: Recomposición local usando Layout Inspector. Un toque a la pantalla debería registrar 0 recomposiciones extra. Agregar un producto debería recomponer solo el área del carrito.
- **Gradle**: Build exitoso tras incorporar `kotlinx.collections.immutable`.
- **Manual**: Probar escaneo de productos por barcode, flujo de inactividad, split accounts y carrito para validar que todo funcione normal sin regresiones visuales.

## Criterio para No Continuar

- Si por alguna razón Gradle rechaza la versión de la librería inmutable, pausar y consultar alternativa con un Wrapper `@Immutable` local.
