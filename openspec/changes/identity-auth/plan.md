# Plan Tecnico: User Identity and Auth Audit

Spec ID: `identity-auth`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Archivos propietarios del flujo real antes de editar codigo.

## Punto de Entrada Real

Describe donde vive hoy el comportamiento y por que ese es el owner correcto.

- UI: N/A
- ViewModel: `AuthViewModelV2.kt`, `SessionViewModel.kt`
- Dominio/use case: `AuthorizationManager.kt`
- Repositorio/data: `AuthRepository.kt` (mencionado en ViewModels)
- DI: `AppModules.kt`
- Tests: N/A

## Archivos Probables

- `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/AuthViewModelV2.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SessionViewModel.kt`

## Estrategia

1. Analizar el `AuthorizationManager` para entender los roles (`ADMIN`, `DUEÑO`), permisos granulares (`PermisoEmpleado`), control de acceso mediante PIN, limite de tasa (Rate Limiting), y bitacora de auditoria.
2. Analizar `AuthViewModelV2` para entender el flujo de login con Firebase Auth.
3. Analizar `SessionViewModel` para entender el manejo de la sesion, cache local (`bocatta_session`), sucursales, modos manuales, y estado de conexion.
4. Documentar los hallazgos sin realizar modificaciones en el codigo fuente.

## Contratos a Revisar

- Modelos: `Usuario`, `PermisoEmpleado`, `EmpleadoV2`, `PinAuthorization`.
- Interfaces: `PinRateLimitStore`.
- Koin: Resolucion de `AuthorizationManager`, `AuthViewModelV2`, y `SessionViewModel`.
- Firestore/Room: Colecciones `usuarios`, `empleados`, `pin_authorizations`, `v2_auditoria_empleados`.
- WorkManager/sync: N/A

## Impacto Offline

- Venta online: N/A
- Venta offline: N/A
- Reconexion: N/A
- Deduccion de inventario: N/A
- Idempotencia/reintentos: N/A

## Impacto UI

- Movil: N/A
- Tablet: N/A
- Estados de carga/error: N/A
- Eventos consumibles: N/A
- Accesibilidad: N/A

## Plan de Pruebas

- Unit: N/A
- Instrumented: N/A
- Compose UI: N/A
- Maestro/manual: N/A
- Gradle: N/A

## Criterio para No Continuar

Enumera condiciones que deben detener implementacion hasta aclarar alcance o datos.

- Si se detecta un problema critico de seguridad durante la auditoria, se debe detener y reportar antes de continuar con cualquier otra tarea.
