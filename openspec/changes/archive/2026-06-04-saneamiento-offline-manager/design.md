# Design: Saneamiento de Offline Manager

Spec ID: `design-saneamiento-offline-manager`

## Resumen Técnico

- Limpieza quirúrgica de código deprecado e inactivo en `OfflineManager.kt`.
- El archivo se reduce en líneas de código, reduciendo la complejidad ciclomática general del módulo de sincronización.
- No hay dependencias externas de documentación afectadas.

## Dependencias de Documentación Externa

- No aplica porque: Se trata de una refactorización interna y retiro de código local marcado como obsoleto, sin consumo de APIs externas ni cambios en librerías de soporte.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio técnico | Prueba |
|-----------|--------------|----------------|--------|
| R1 (Remover obsoletos) | `OfflineManager.kt` | Borrar `generarCodigoTicket`, `obtenerUltimoTicketLocal`, `guardarUltimoTicketLocal`, y `leerUltimoTicketLocalLegacy` | Compilación de Gradle y validación de referencias |
| R2 (Preservar guardado offline) | `OfflineManager.kt` | Preservar `guardarVentaOffline` y `guardarOperacionOffline` | Compilación limpia de Kotlin |

## APIs, Contratos y Datos

- Modelos: Sin cambios.
- Interfaces: Sin cambios.
- Repositorios: Sin cambios.
- Use cases: Sin cambios.
- DI/Koin: Sin cambios.
- Firestore: Sin cambios.
- Room/SQLite: Sin cambios.
- WorkManager: Sin cambios.

## Estructuras de Salida

No aplica.

## Edge Cases

- **Datos corruptos/incompletos**: No aplica. La remoción no toca flujos de lectura/escritura activos.

## Guardrails Aplicados

- [x] Negocio fuera de Composables.
- [x] Estado en ViewModel.
- [x] Repositorios/use cases existentes reutilizados.
- [x] Offline auditable si aplica.
- [x] Deducción de inventario validada si aplica.

## Checkpoint Humano

Aprobado por el usuario ("adelante con lo demas siempre usa el sdd antes de hacer cambios").
