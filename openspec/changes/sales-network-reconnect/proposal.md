# Proposal: Reconectar estado online del mostrador

Spec ID: `BCT-F14-NETWORK`
Capability: `sales-connectivity-state`
Strict TDD: `false`

## Problema

- El encabezado de ventas puede quedar en offline despues de recuperar internet.
- El bloqueo por inactividad y otros estados operativos consumen el mismo booleano.

## Alcance

- Observar el Flow existente de `NetworkStateProvider` desde `SalesViewModelV2`.
- Mantener `isOnline` sincronizado durante toda la vida del ViewModel.

## Fuera de Alcance

- Migracion multimodulo, `Routes`, DI, Gradle y navegacion.
- Cambios a WorkManager, Firestore o persistencia offline.
- Rediseno visual.

## Archivos Afectados Esperados

- `feature/ventas/.../SalesViewModelV2.kt`
- Artefactos SDD y `notas.txt`.

## Riesgos

- Ventas: bajo; no cambia checkout.
- Inventario: ninguno.
- Offline/sync: positivo; la UI refleja la reconexion.
- Caja/pagos: ninguno.
- UI: recomposicion puntual del encabezado.
- Seguridad: ninguno.

## Governance Gate

- Hay mas de tres cambios SDD abiertos. Se ignora el soft gate porque este defecto
  fue reproducido en AVD, afecta directamente la operacion de mostrador y la
  correccion es de un solo owner sin cruzar la migracion.

## Estrategia de Rollback

- Retirar el collector agregado al ViewModel.

## Criterios de Exito

- [ ] `isOnline` cambia al perder red sin requerir checkout.
- [ ] `isOnline` vuelve a true al recuperar una red validada.
- [ ] No se modifican contratos, DI ni logica de venta.

## Checkpoint Humano

- Aceptado por la instruccion explicita del usuario de continuar con lo no relacionado
  a migracion.
