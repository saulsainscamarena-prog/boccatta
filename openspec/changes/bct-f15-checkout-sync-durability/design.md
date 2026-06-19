# Design: Checkout Sync Durability

Spec ID: `bct-f15-checkout-sync-durability`

## Resumen Tecnico

Se reutilizan owners actuales: `SalesViewModelV2` coordina estado de UI, `CheckoutUseCase` decide persistencia online/offline, `FirebaseSalesRepositoryV2` escribe online, `OfflineManager` guarda local y `SyncWorker` reconcilia.

## Dependencias de Documentacion Externa

- Android/Compose: no aplica.
- Firebase: no aplica.
- Gradle/JDK: no aplica.
- Kotlin/Koin: no aplica.
- Seguridad/Play Store: no aplica.
- No aplica porque: no se introducen APIs externas ni dependencias.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio tecnico | Prueba |
|-----------|--------------|----------------|--------|
| R1 | `SalesViewModelV2` | limpiar con snapshot tras exito | test/AVD |
| R2 | `CheckoutUseCase` | usar classifier y fallback offline | unit |
| R3 | repositorio/sync | leer venta existente antes de escribir | unit/static/compile |
| R4 | `RegistrarCancelacionUseCase` | inyectar y usar antes de limpiar | unit |

## APIs, Contratos y Datos

- Modelos: ampliar `ResultadoVenta` con `alreadyExisted` por defecto.
- Interfaces: mantener `SalesRepository`.
- Repositorios: idempotencia en venta online.
- Use cases: `CheckoutResult` agrega `queuedOffline`, `failureKind`.
- DI/Koin: agregar `RegistrarCancelacionUseCase` al modulo principal y `SalesDependencies`.
- Firestore: no cambiar colecciones.
- Room/SQLite: no cambiar schema.
- WorkManager: `KEEP` para sync inmediato.

## Estructuras de Salida

No cambia payload publico. Cancelacion usa payload JSON existente del use case.

## Edge Cases

- Sin internet: offline directo.
- Reconexion: venta remota existente se marca sincronizada.
- Doble toque/reintento: `cargando` bloquea y finally libera.
- Rol no autorizado: error permanente sin fallback.
- Datos corruptos/incompletos: error permanente sin fallback.

## Guardrails Aplicados

- [x] Negocio fuera de Composables.
- [x] Estado en ViewModel.
- [x] Repositorios/use cases existentes reutilizados.
- [x] Offline auditable si aplica.
- [x] Deduccion de inventario validada si aplica.
