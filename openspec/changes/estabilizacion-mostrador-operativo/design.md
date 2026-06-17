# Design: Estabilizacion integral de mostrador

Spec ID: `BCT-F15-MOSTRADOR-STABLE`

## Resumen Tecnico

Se conserva el flujo existente y se agregan contratos pequenos:

1. Clasificador compartido de fallos de persistencia.
2. Fallback local solo para fallos transitorios/ambiguos.
3. Resultado de sync que reconcilia IDs y tickets existentes.
4. Resumen tipado de colas offline.
5. Ruta unica y durable para cancelaciones.

## Dependencias de Documentacion Externa

- Android/Compose: no aplica; patrones locales y skills recientes.
- Firebase: no se cambia SDK ni reglas en esta fase.
- Gradle/JDK: no se cambia configuracion.
- Kotlin/Koin: no se cambian versiones.
- Seguridad/Play Store: no aplica.

La API experimental Compose Styles no se adoptara porque exige opt-in y cambios
de dependencias. La UI usara `MaterialTheme.colorScheme`, tipografia y shapes
existentes.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio tecnico | Prueba |
|-----------|--------------|----------------|--------|
| R1-R3 | CheckoutUseCase | clasificacion y fallback selectivo | unit |
| R4 | SyncWorker/OfflineDatabase | resultado existente + reconciliacion | worker/SQLite |
| R5 | SyncWorker | transitorio permanece pendiente | unit |
| R6-R7 | data/offline + Sales VM | `OfflineQueueSummary` observable | unit |
| R8 | ventas use case/VM | cancelacion durable antes de limpiar | unit |
| R9 | DialogosVentas | exacto decimal y chips consistentes | Compose |
| R10 | AdminScreen | hub operativo de inventario | Compose/AVD |

## APIs, Contratos y Datos

- Modelos:
  - `SalePersistenceFailure`: `Transient`, `Permanent`, `Business`.
  - `OfflineQueueSummary`: conteos por tipo, fallos y error de lectura.
  - `SaleSyncOutcome`: `Created` o `AlreadyExists` con ticket remoto.
- Interfaces:
  - Extender almacenamiento con consultas de resumen y reconciliacion.
  - Agregar conteo pendiente a `IStockAdjustmentQueue` si no existe.
- Repositorios:
  - Reutilizar `SalesRepository`; no crear otro repositorio de ventas.
- Use cases:
  - `CheckoutUseCase` sigue siendo owner.
  - Crear `RegistrarCancelacionUseCase` solo si permite eliminar IO directo del
    ViewModel para item y carrito total.
- DI/Koin:
  - Cambios minimos en `DataModule` y `VentasModule`, despues de que la
    migracion estabilice DI.
- Firestore:
  - Mismo documento `v2_ventas/{forcedVentaId}`.
- Room/SQLite:
  - No borrar registros hasta estado sincronizado.
  - Reconciliar `ticket` y `codigoTicket` remoto cuando exista.
- WorkManager:
  - Permanente: terminal y diagnostico.
  - Transitorio: pendiente + `Result.retry()` con backoff.

## Estructura de Resumen

```text
OfflineQueueSummary(
  pendingSales,
  failedSales,
  pendingOperations,
  pendingStockAdjustments,
  pendingContingencyShifts,
  readError
)
```

## Edge Cases

- Sin internet desde el inicio: guardar local directamente.
- Red cae durante commit: guardar local con el mismo ID.
- Commit remoto concluye pero se pierde respuesta: worker detecta documento,
  obtiene ticket y no repite stock.
- Doble toque: `cargando` y estado de UI bloquean segunda ejecucion.
- Rol no autorizado: no se registra cancelacion.
- Datos corruptos: terminal visible y exportable en diagnostico.

## Guardrails Aplicados

- [x] Negocio fuera de Composables.
- [x] Estado en ViewModel.
- [x] Repositorios/use cases existentes reutilizados.
- [x] Offline auditable.
- [x] Deduccion de inventario sin cambiar formulas.

## Checkpoint Humano

El diseño queda listo para ejecución por fases. La fase de UX administrativa
requiere captura AVD antes de aceptar su diseño final.
