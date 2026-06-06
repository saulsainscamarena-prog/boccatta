# Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`
Capability: `mostrador-offline-sync-auth-ticket`
Fecha: `2026-06-06`

## Problema

El mostrador real necesita operar sin bloqueos evitables. La auditoria encontro riesgos en permisos de turno, cola offline de stock, payload de sincronizacion, auditabilidad de tickets y manejo de undo. Los afectados directos son vendedor, admin, soporte y sincronizacion offline.

## Objetivos

- Evitar que un vendedor autenticado quede bloqueado al abrir turno por reglas Firestore sobre listados de empleados.
- Recuperar ajustes de stock offline en estado `SYNCING` antiguo para que el worker pueda reintentarlos.
- Sincronizar ajustes offline siempre en unidad base para evitar inconsistencias de inventario.
- Mostrar descuento manual en ticket WhatsApp.
- Mantener undo del carrito en un solo owner.
- Mantener cambios pequenos, auditables y sin tocar Gradle.

## No Objetivos

- Redisenar `AdminScreen` o conectar `AjustesAlmacenScreen` a navegacion.
- Cambiar reglas Firestore.
- Cambiar schemas Room/SQLite.
- Reescribir checkout o `SalesViewModelV2`.
- Resolver deuda global de detekt/ktlint/CI.
- Usar Play Store como prioridad.

## Usuarios y Flujos

### Flujo principal

1. Vendedor inicia sesion y abre turno.
2. Vendedor vende y genera ticket.
3. Si hay operaciones offline, WorkManager sincroniza al recuperar red.
4. Soporte/admin revisa logs/tickets con datos auditables.

### Escenarios alternos

- Sin internet: ajustes de stock quedan en cola local y deben poder reintentarse.
- Reintento/sync: estados `SYNCING` obsoletos se devuelven a pendientes antes de sincronizar.
- Error visible: esta fase no cambia UI de errores; preserva rutas existentes.
- Permisos/rol: `VENDEDOR` puede abrir turno; permisos mas sensibles siguen pasando por validaciones existentes.

## Requisitos Funcionales

- R1: `AuthorizationManager.verificarPermiso` debe permitir `Rol.VENDEDOR` + `AccionSensible.ABRIR_TURNO` sin list query a `v2_employees`.
- R2: `SyncWorker` debe llamar a recuperacion de ajustes `SYNCING` viejos antes de leer ventas/ajustes pendientes.
- R3: `InventoryRepositoryImpl.syncOfflineAdjustment` debe convertir cantidades no-base con `UnitConverter.toBase` y escribir movimiento en unidad base.
- R4: `GenerarTicketWhatsAppUseCase` debe aceptar descuento manual opcional y mostrarlo si es mayor a cero.
- R5: `CheckoutUseCase` debe pasar descuento manual al generador de ticket.
- R6: `CartManager` debe poseer el stack de undo y `SalesViewModelV2` debe delegarlo.

## Requisitos No Funcionales

- Rendimiento de mostrador: no agregar IO ni recomposicion a Composables.
- Persistencia/offline: no borrar ni marcar completado trabajo offline antes de sync remoto.
- Auditabilidad: ticket muestra descuento manual aplicado.
- Seguridad/permisos: no ampliar permisos admin; abrir turno es el unico fast path para vendedor.
- Accesibilidad: no aplica, no hubo UI Compose nueva.

## Criterios de Aceptacion

- [x] Test de permiso confirma que vendedor abre turno sin consultar Firestore.
- [x] Test de contrato confirma que no hay query `whereEqualTo("authUid")` en el bloque de permiso.
- [x] Test de contrato confirma recuperacion de stale `SYNCING` en `SyncWorker`.
- [x] Test de contrato confirma conversion a unidad base en sync offline.
- [x] Test unitario confirma descuento manual en ticket.
- [ ] Verificacion Gradle final documentada.

## Riesgos POS

- Ventas: el checkout sigue protegido por rutas existentes; no cambia persistencia de venta.
- Inventario: conversion a base reduce riesgo de cantidades remotas equivocadas.
- Offline/sync: recuperacion stale evita que ajustes queden invisibles para siempre.
- Caja/pagos: descuento en ticket mejora conciliacion.
- Reportes/tickets: salida WhatsApp cambia solo cuando hay descuento manual positivo.

## Preguntas Abiertas

- Conectar administracion real de stock sigue pendiente en una spec posterior.
- Limpiar frontera de `CheckoutUseCase` con dependencias data/Android sigue pendiente.
- Ejecutar AVD completo sigue pendiente si el emulador y Gradle estan libres.
