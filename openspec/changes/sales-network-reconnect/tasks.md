# Tasks: Reconectar estado online del mostrador

Spec ID: `BCT-F14-NETWORK`

## Tareas

- [x] 1. Confirmar owner actual del estado.
  - Archivo: `SalesViewModelV2.kt`.
  - Verificacion: busqueda de usos de `networkStateProvider`.
- [x] 2. Observar el Flow en `viewModelScope`.
  - Archivo: `SalesViewModelV2.kt`.
  - Verificacion: revision de diff.
- [x] 3. Ejecutar compilacion aislada del modulo.
  - Comando: `.\gradlew.bat :feature:ventas:compileDebugKotlin --no-parallel --max-workers=1`
  - Resultado: exitoso.
- [ ] 3.1 Ejecutar compilacion de la APK completa.
  - Comando: `.\gradlew.bat assembleDebug --no-parallel --max-workers=1`
  - Bloqueo inicial: `MainActivity.kt` no resolvia `Routes`.
  - Bloqueo actual: `CarritoPanelV2.kt:169` no resuelve `Icons.Default.Star`
    despues de cambios concurrentes.
- [ ] 4. Revalidar reconexion en AVD.
  - Verificacion: encabezado online/offline y logs.
- [ ] 5. Completar review y verification.

## Checklist Critico

- [x] Sin cambios de checkout o inventario.
- [x] Sin dependencias nuevas.
- [x] Sin archivos de migracion.
- [ ] Error visible y estado coherente tras reconexion.
