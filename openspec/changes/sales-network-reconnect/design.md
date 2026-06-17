# Design: Reconectar estado online del mostrador

Spec ID: `BCT-F14-NETWORK`

## Resumen Tecnico

- `SalesViewModelV2` ya recibe `NetworkStateProvider`.
- Se agrega un collector unico en `init` usando `viewModelScope`.
- Se reutiliza el `mutableStateOf` existente para no cambiar la API consumida por
  `SalesScreen`.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio tecnico | Prueba |
|-----------|--------------|----------------|--------|
| R1-R3 | SalesViewModelV2 | collect de isOnline en viewModelScope | AVD y compilacion |

## APIs, Contratos y Datos

- Sin cambios de firmas, DI, Firestore, Room o WorkManager.

## Edge Cases

- Emisiones repetidas: `NetworkStateProvider` ya usa `distinctUntilChanged`.
- Destruccion del ViewModel: `viewModelScope` cancela el collector.
- Firestore no disponible con red validada: checkout conserva su manejo de errores.

## Guardrails Aplicados

- [x] Estado en ViewModel.
- [x] Flow existente reutilizado.
- [x] Sin IO en Composables.
- [x] Sin arquitectura paralela.

## Checkpoint Humano

- Aceptado por la instruccion de continuar fuera de migracion.
