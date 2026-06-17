# Reconectar estado online del mostrador

Spec ID: `BCT-F14-NETWORK`
Capability: `sales-connectivity-state`
Fecha: `2026-06-12`

## Objetivos

- Reflejar en el mostrador el estado emitido por `NetworkStateProvider`.
- Recuperar automaticamente el estado online tras una reconexion.

## No Objetivos

- Probar disponibilidad real de Firestore.
- Cambiar la politica de persistencia o sincronizacion.

## Requisitos Funcionales

- R1: El ViewModel debe observar el Flow de conectividad al inicializarse.
- R2: Cada emision debe actualizar el estado durable `isOnline`.
- R3: La observacion debe cancelarse con `viewModelScope`.

## Escenarios

### Perdida de conectividad

Given el mostrador esta online
When `NetworkStateProvider` emite false
Then `SalesViewModelV2.isOnline` debe ser false sin ejecutar una venta.

### Reconexion

Given el mostrador esta offline
When `NetworkStateProvider` emite true
Then `SalesViewModelV2.isOnline` debe volver a true sin reiniciar la pantalla.

## Criterios de Aceptacion

- [ ] El encabezado puede transicionar online -> offline -> online.
- [ ] No hay polling ni consultas de red desde Compose.
- [ ] No cambia el resultado del checkout.
