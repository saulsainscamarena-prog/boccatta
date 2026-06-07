# Proposal: Refactor de Arquitectura (ViewModel y Data)

## Objetivo
Mejorar la separación de responsabilidades y la mantenibilidad de la aplicación Bocatta POS sin comprometer el rendimiento en mostrador ni la robustez de las ventas offline.

## Objetivos (In-Scope)
- Extraer las consultas y listeners de Firestore (Data) fuera de los ViewModels (Presentación).
- Mover toda suscripción de red a Repositorios que devuelvan `Flow<T>`.
- Refactorizar `SalesViewModelV2` para que dependa exclusivamente de Casos de Uso y Repositorios para lectura/escritura de datos.
- Evaluar la transición controlada y progresiva de `SQLiteOpenHelper` (`OfflineDatabase.kt`) a **Android Room** para tener comprobación de tipos y seguridad en tiempo de compilación para el almacenamiento offline.

## No Objetivos (Out-of-Scope)
- Reescribir la lógica completa de ventas o el sistema de carritos.
- Alterar la lógica matemática de deducción de inventario o cálculo de promociones.
- Modificar el sistema de Inyección de Dependencias (Koin) más allá de registrar los nuevos Flows/Repositorios.

## Criterios de Aceptación
1. `SalesViewModelV2` no debe importar clases de Firestore (ej. `ListenerRegistration`, `FirebaseFirestoreProvider`).
2. Las listas de menú (`productos`) y alertas de stock deben consumirse vía `Flow` desde `IProductRepository` e `IInventoryRepository`.
3. Las pruebas unitarias existentes (`*Koin*`, `*Offline*`) deben seguir pasando sin modificaciones de comportamiento.

## Riesgos y Mitigación
- **Riesgo:** Perder eventos o desfasar la UI por una mala gestión del `Flow` de Firestore.
  - *Mitigación:* Utilizar `stateIn` o `shareIn` en los ViewModels con `SharingStarted.WhileSubscribed` para asegurar que las conexiones a Firestore se cierren cuando la UI no está visible.
- **Riesgo:** Romper la atomicidad de las operaciones offline en SQLite si se migra a Room.
  - *Mitigación:* Si se aprueba la migración a Room, hacerlo usando la anotación `@Transaction` para replicar el comportamiento de `beginTransaction()` en `OfflineDatabase`. Se debe mantener `OfflineDatabase.kt` como legacy hasta verificar Room exhaustivamente.
