# Proposal: Saneamiento de Offline Manager

Spec ID: `saneamiento-offline-manager`
Capability: `general`
Strict TDD: `true`

## Problema

- `OfflineManager.kt` contiene funciones obsoletas marcadas como `@Deprecated` que manejan persistencia de folios/tickets en `SharedPreferences` de forma no atómica.
- Se mantiene esta deuda técnica descrita en `TECH_DEBT.md` sin aportar valor operativo, ensuciando la compilación con advertencias de desaprobación.
- El usuario desea saneamiento limpio y ordenado utilizando el flujo SDD.

## Alcance

- Remover quirúrgicamente las siguientes funciones de [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt):
  - `generarCodigoTicket`
  - `obtenerUltimoTicketLocal`
  - `guardarUltimoTicketLocal`
  - `leerUltimoTicketLocalLegacy`
- Mantener las funciones operativas de guardado offline transaccional y registro de operaciones.

## Fuera de Alcance

- Modificar la lógica de persistencia de folios SQLite en `OfflineDatabase.kt`.
- Cambios de configuración de memoria en `gradle.properties` (el usuario indicó explícitamente mantener los 4GB actuales).

## Archivos Afectados Esperados

- [OfflineManager.kt](file:///c:/Users/ia/AndroidStudioProjects/bocatta/app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt)

## Riesgos

- **Ventas**: Ninguno. El flujo de ventas ya no consulta estas funciones obsoletas.
- **Inventario**: Ninguno.
- **Offline/sync**: Ninguno. La sincronización se realiza mediante folios en SQLite.
- **Caja/pagos**: Ninguno.
- **UI**: Ninguno.
- **Seguridad**: Ninguno.

## Research Externo

- Web requerida: no
- Pregunta investigada: N/A
- Fuentes primarias: N/A
- Decision tomada: Local-only.
- Suposicion sensible a version: N/A

## Estrategia de Rollback

- Revertir los cambios locales mediante git:
  `git checkout -- app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt`

## Criterios de Exito

- [ ] `OfflineManager.kt` no contiene métodos obsoletos ni referencias a SharedPreferences de folios legacy.
- [ ] La aplicación compila correctamente sin errores de compilación por símbolos faltantes en otras clases o pruebas unitarias.
- [ ] Se ejecuta exitosamente la prueba de Mojibake tras aplicar la edición.

## Checkpoint Humano

Aprobado por el usuario ("adelante con lo demas siempre usa el sdd antes de hacer cambios").
