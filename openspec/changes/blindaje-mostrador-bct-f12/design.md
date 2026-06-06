# Design: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`

## Resumen Tecnico

- Cambios quirurgicos en owners existentes.
- Sin nuevos repositorios, schedulers, Composables ni dependencias.
- Tests focalizados para contratos criticos donde no hay fake completo de Firestore/Room.

## Dependencias de Documentacion Externa

- Android/Compose: no aplica.
- Firebase: no aplica, no se cambiaron reglas ni inicializacion.
- Gradle/JDK: no aplica, no se tocaron archivos Gradle.
- Kotlin/Koin: no aplica, no se cambio DI.
- Seguridad/Play Store: no aplica.
- No aplica porque: la decision depende de codigo local y reglas de negocio ya existentes.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio tecnico | Prueba |
|-----------|--------------|----------------|--------|
| R1 | `AuthorizationManager` | Retorno directo para vendedor al abrir turno; helper `obtenerPermisoEmpleadoActual` usa `document(uid)` | `AuthorizationManagerTest`, `PinAuthorizationRegressionTest` |
| R2 | `SyncWorker` + `IStockAdjustmentQueue` | `resetStaleSyncing(STALE_SYNCING_MAX_AGE_MS)` al inicio del worker | `OfflineOperationsContractTest` |
| R3 | `InventoryRepositoryImpl` | Conversion a base con `UnitConverter.toBase`; movimiento remoto en unidad base | `OfflineOperationsContractTest` |
| R4 | `GenerarTicketWhatsAppUseCase` | Parametro `descuentoManual` con default y linea en ticket | `GenerarTicketWhatsAppUseCaseTest` |
| R5 | `CheckoutUseCase` | Pasa descuento manual al ticket; stock usa `Dispatchers.IO`; conserva estado online real si falla stock | Compilacion y tests de ticket |
| R6 | `CartManager` + `SalesViewModelV2` | Undo centralizado en `CartManager`; VM delega | Compilacion y pruebas manuales de carrito pendientes |

## APIs, Contratos y Datos

- Modelos: sin cambios.
- Interfaces: se reutiliza `IStockAdjustmentQueue.resetStaleSyncing`.
- Repositorios: `InventoryRepositoryImpl.syncOfflineAdjustment` cambia payload interno.
- Use cases: `GenerarTicketWhatsAppUseCase` agrega parametro default compatible; `CheckoutUseCase` lo consume.
- DI/Koin: sin cambios.
- Firestore: payload de ajuste remoto conserva `currentQty`/`cantidadEnBase` y movimiento base.
- Room/SQLite: sin schema nuevo.
- WorkManager: `SyncWorker` recupera estados stale antes de procesar.

## Estructuras de Salida

Ticket WhatsApp cuando existe descuento manual:

```text
Desc. manual: -$10.00
```

Movimiento remoto de ajuste offline:

```json
{
  "quantity": 1.0,
  "unit": "base",
  "userId": "offline_sync"
}
```

## Edge Cases

- Sin internet: cola local mantiene ajustes pendientes.
- Reconexion: worker recupera `SYNCING` viejo y reintenta.
- Doble toque/reintento: no se cambia checkout; undo no duplica estados al eliminar.
- Rol no autorizado: solo vendedor con accion abrir turno tiene permiso directo.
- Datos corruptos/incompletos: no se ocultan nuevas excepciones; se preserva manejo existente.

## Guardrails Aplicados

- [x] Negocio fuera de Composables.
- [x] Estado en ViewModel.
- [x] Repositorios/use cases existentes reutilizados.
- [x] Offline auditable si aplica.
- [x] Deduccion de inventario validada si aplica.

## Checkpoint Humano

El usuario pidio ejecutar la fase. No se requiere pausa salvo fallo Gradle, conflicto de procesos o evidencia de regresion critica.
