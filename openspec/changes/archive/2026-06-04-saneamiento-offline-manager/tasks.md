# Tasks: Saneamiento de Offline Manager

Spec ID: `saneamiento-offline-manager`

## Reglas

- Cada tarea debe ser pequeña, auditable y reversible.
- No mezclar refactor amplio con corrección funcional.
- Marcar verificación real por tarea o explicar por qué no aplica.

## Tareas

- [x] 1. Eliminar `@Deprecated fun generarCodigoTicket` en `OfflineManager.kt`.
  - Archivos: `OfflineManager.kt`
  - Verificación: Compilación y revisión visual.

- [x] 2. Eliminar `@Deprecated fun obtenerUltimoTicketLocal` y `@Deprecated fun guardarUltimoTicketLocal` en `OfflineManager.kt`.
  - Archivos: `OfflineManager.kt`
  - Verificación: Compilación y revisión visual.

- [x] 3. Eliminar la función helper privada `leerUltimoTicketLocalLegacy` en `OfflineManager.kt`.
  - Archivos: `OfflineManager.kt`
  - Verificación: Compilación y revisión visual.

- [x] 4. Limpiar imports obsoletos e inactivos resultantes del retiro.
  - Archivos: `OfflineManager.kt`
  - Verificación: Compilación sin warnings de imports.

- [x] 5. Ejecutar verificación de Mojibake y compilar.
  - Comando: `python C:\Users\ia\.gemini\antigravity\brain\3b8affe2-da2c-415e-b85f-74740386cf35\scratch\audit_runner.py`
  - Resultado: Preflight OK. No se detectó mojibake.

## Checklist Crítico

- [x] Checkout protegido contra doble toque si toca ventas/pagos (N/A).
- [x] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [x] Deducción de inventario validada online/offline si aplica.
- [x] Errores visibles, sin fallos silenciosos.
- [x] No se agregaron dependencias duplicadas.
- [x] No se tocaron archivos fuera del alcance.
