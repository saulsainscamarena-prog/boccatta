# Saneamiento de Offline Manager

Spec ID: `saneamiento-offline-manager`
Capability: `general`
Fecha: `2026-06-04`

## Problema

La presencia de métodos deprecados de Shared Preferences en `OfflineManager.kt` ensucia la base de código y genera warnings durante el proceso de compilación, sin aportar ninguna utilidad ya que el almacenamiento de folios e historial offline está totalmente migrado a la base de datos SQLite transaccional.

## Objetivos

- Remover de forma limpia y auditada las declaraciones obsoletas en `OfflineManager.kt`.
- Asegurar que no existan regresiones en otras clases o pruebas unitarias del POS.

## No Objetivos

- Cambiar la lógica funcional del flujo offline en SQLite ni la sincronización con Firestore.
- Modificar archivos de persistencia o repositorios de datos activos.

## Usuarios y Flujos

### Flujo Técnico de Compilación
1. El compilador de Kotlin/Gradle procesa el paquete `com.bocatta.pos.data.sync`.
2. Se eliminan las advertencias relativas al uso de las SharedPreferences deprecadas del POS.
3. Se verifican las clases dependientes para garantizar un comportamiento idéntico en pruebas y checkout.

## Requisitos Funcionales

- **R1**: Eliminación de `generarCodigoTicket`, `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal`, y `leerUltimoTicketLocalLegacy`.
- **R2**: El guardado offline de ventas y operaciones de caja debe seguir invocando el generador de folios SQLite en `OfflineDatabase` sin interrupción.

## Requisitos No Funcionales

- **Rendimiento de mostrador**: N/A (el cambio remueve código inactivo, lo que reduce mínimamente el tamaño del binario compilado).
- **Persistencia/offline**: Debe mantenerse la atomicidad del checkout offline en base a SQLite.
- **Auditabilidad**: Los folios generados deben seguir el formato `SUCURSAL-TICKET` provisto por `TicketUtils`.

## Criterios de Aceptación

- [ ] Código libre de funciones deprecated en `OfflineManager.kt`.
- [ ] Compilación exitosa del módulo POS.
- [ ] Verificación de mojibake limpia.

## Riesgos POS

- **Ventas**: Mínimo. Se mitiga habiendo verificado mediante grep la ausencia absoluta de llamadas activas a estas SharedPreferences obsoletas.
- **Inventario**: N/A.
- **Offline/sync**: Mínimo. Mitigado por el uso de `OfflineDatabase` y `SyncWorker` transaccional.
- **Caja/pagos**: N/A.
- **Reportes/tickets**: Mínimo. Mitigado por la invocación directa a `TicketUtils` para generar el código de ticket en formato WhatsApp.

## Preguntas Abiertas

- Ninguna. El alcance es puramente técnico y seguro.
