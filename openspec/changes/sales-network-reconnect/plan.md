# Plan Tecnico: Reconectar estado online del mostrador

Spec ID: `BCT-F14-NETWORK`

## Punto de Entrada Real

- UI: `SalesScreenSections` renderiza `isOnline`.
- ViewModel: `SalesViewModelV2` posee el estado, pero no observa el provider.
- Red: `NetworkStateProvider` ya expone el Flow requerido.

## Estrategia

1. Agregar import de `collect`.
2. Iniciar un collector unico en `SalesViewModelV2`.
3. No cambiar checkout ni provider.
4. Compilar e instalar cuando la migracion permita `assembleDebug`.
5. Validar corte y reconexion en AVD.

## Plan de Pruebas

- Gradle: `assembleDebug`.
- Manual AVD: online -> airplane mode -> offline -> recuperar red -> online.

## Criterio para No Continuar

- Si requiere tocar `Routes`, DI o archivos del cambio multimodulo, detener y dejar
  la verificacion pendiente al agente de migracion.
