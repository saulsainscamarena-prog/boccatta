# Research: Saneamiento de Offline Manager

Spec ID: `saneamiento-offline-manager`
Capability: `general`
Fecha: `2026-06-04`

## Brief Humano

- El usuario desea realizar la limpieza técnica (saneamiento) recomendada en la auditoría.
- Se eliminarán las funciones obsoletas de Shared Preferences de folios de ticket en `OfflineManager.kt` que están deprecadas desde que el sistema migró al manejo de folios atómico en SQLite (`folios_offline`).
- El usuario decidió mantener la configuración de memoria de 4GB en `gradle.properties` ("4 gb esta bien").
- Se requiere que el agente use el flujo SDD antes de realizar cambios de código.

## Web Research Gate

- Research question: ¿Existen referencias en dependencias externas o librerías del SDK que requieran SharedPreferences deprecadas en `OfflineManager`?
- Source category: None.
- Why local context is sufficient: El comportamiento de persistencia de folios es estrictamente local de Bocatta POS. Hicimos búsquedas con grep confirmando que ninguna otra clase del proyecto ni archivos de test hacen uso de las funciones deprecadas `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal`, o `generarCodigoTicket` de `OfflineManager`. Toda la app utiliza la clase centralizada `TicketUtils` y las tablas de `OfflineDatabase`.
- Expected decision: Proceder con la remoción quirúrgica de las funciones obsoletas.
- Budget: none

## Contexto Local Leido

- Constitution: Regla de mantener cambios pequeños, auditables y sin refactors amplios innecesarios.
- AGENTS: "No reviertas cambios existentes del usuario. Todo cambio debe ser pequeño, auditable y fácil de revisar."
- Archivos inspeccionados: `OfflineManager.kt`, `OfflineDatabase.kt`, `TicketUtils.kt`, `InfraestructuraTest.kt`, `TicketUtilsTest.kt`.

## Alternativas

### Opción A (Recomendada)
- Descripción: Remover los métodos deprecados y su helper privado en `OfflineManager.kt`.
- Ventajas: Sanea el archivo reduciendo ruido de compilación y remueve código muerto obsoleto.
- Costos: Ninguno.
- Riesgos: Ninguno, ya que no hay dependencias de llamadas.

### Opción B
- Descripción: Mantener las funciones obsoletas con `@Deprecated` y advertencias de compilación.
- Ventajas: Retrocompatibilidad pasiva.
- Costos: Mantiene la deuda técnica activa en `TECH_DEBT.md`.
- Riesgos: Confusión en futuras integraciones.

## Decisión Recomendada

- Opción A: Remoción quirúrgica de los métodos deprecados de Shared Preferences en `OfflineManager.kt`.

## Suposiciones Externas

- El sistema de folios basado en SQLite (`folios_offline`) ya está verificado en producción y tests instrumentados, por lo que no hay riesgo de pérdida de datos.

## Tradeoffs

- El código se vuelve más compacto y limpio a nivel de API interna.
