# Reporte de auditoria de codigo muerto - 2026-05-21

## Resultado

- `compileDebugKotlin`: exitoso despues de aislar los candidatos.
- `:app:lintDebug`: exitoso, `0 errors, 63 warnings, 23 hints`.
- `:app:testDebugUnitTest`: exitoso.

## Codigo aislado

Los siguientes archivos se movieron fuera de `app/src` como `.txt`, dentro de esta carpeta temporal:

- `Arquitectura.txt`: documento sin extension dentro de `app/src/main/java`.
- `CostCalculator.kt.txt`: motor de costo sin callers productivos.
- `FirestoreSeeder.kt.txt`: stub legacy desactivado sin callers productivos.
- `GestionarPromocionesScreen.kt.txt`: pantalla prototipo sin ruta/caller productivo.
- `InicioDiaScreen.kt.txt`: pantalla sin uso productivo.
- `MasaPostresStep.kt.txt`: flujo legacy reemplazado por `ValidacionStockPremium`.
- `OpcionesProducto.kt.txt`: catalogo hardcodeado sin callers productivos.
- `ProductMappers.kt.txt`: puente legacy deprecated usado solo por su test.
- `ProductMappersTest.kt.txt`: test dedicado al mapper legacy aislado.
- `PurchasesScreen.kt.txt`: pantalla admin de compras no navegable.
- `PurchasesViewModel.kt.txt`: ViewModel exclusivo de `PurchasesScreen`.
- `SuggestionEngine.kt.txt`: generador de sugerencias sin callers productivos.
- `TicketGeneratorV2.kt.txt`: generador de ticket viejo sin callers productivos.
- `TurnoActivoScreen.kt.txt`: pantalla sin ruta/caller productivo.

## Cambios de soporte

- Se retiro el import residual de `InicioDiaScreen` en `MainActivity.kt`.
- Se retiro el binding Koin residual de `PurchasesViewModel` en `AppModules.kt`.
- Se reemplazo `NavDestination.displayName` por `destination.id` en breadcrumbs para evitar `RestrictedApi`.
- Se corrigio el uso de color dinamico con guard `Build.VERSION.SDK_INT >= S` y helper anotado para lint.
- Se reemplazo `Locale.getDefault()` no observable en Composables por `LocalLocale.current.platformLocale`.

## Pendiente no aislado

- `OfflineManager` conserva wrappers deprecated de folios/tickets legacy. No se movieron porque hay lectura interna legacy como piso de migracion.
- `AuthRepository.validarCodigoMaestro()` sigue deprecated y sin callers productivos; puede eliminarse en una limpieza posterior.
- `Routes.Compras` tiene destination pero no hay `navigate(Routes.Compras)` detectado; requiere decision de UX antes de eliminar.
- `COMPRAS_OLD` sigue usado por `MaintenanceRepository`, probablemente para purga/mantenimiento de coleccion vieja.

## Warnings residuales de lint

- Recursos no usados en `colors.xml`, `strings.xml` y launcher drawables.
- `DefaultLocale` en `ComprasScreen.kt`.
- Orden de parametro `modifier` en algunos Composables compartidos.
- Dependencias declaradas directamente en `app/build.gradle.kts` en vez de version catalog.
- Bitmap `logo_bocatta.png` en carpeta `drawable` sin densidad.
- Warnings de versiones disponibles y APIs desaconsejadas.
