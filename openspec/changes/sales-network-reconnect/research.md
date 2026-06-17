# Research: Estado de red en mostrador

Spec ID: `BCT-F14-NETWORK`
Fecha: `2026-06-12`

## Evidencia Local

- AVD `bocatta_tablet_api36`: al cortar la red, el checkout detecto modo offline.
- Tras restaurar conectividad, WorkManager recibio `onCapabilitiesChanged` y ejecuto `SyncWorker`.
- `SalesScreen` continuo mostrando `SISTEMA OFFLINE`.
- `SalesViewModelV2.isOnline` solo se actualiza con `CheckoutResult.online`; no observa
  `NetworkStateProvider.isOnline`.

## Research Externo

- No requerido. El defecto se demuestra con el flujo local y las APIs ya integradas.
- Las skills de conectividad y estado fueron verificadas el 2026-06-08.

## Condicion de Parada

- No modificar `NetworkStateProvider`, DI o la migracion multimodulo salvo que el
  ViewModel no pueda observar el Flow existente.
