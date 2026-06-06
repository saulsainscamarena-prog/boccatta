# Proposal: Blindaje de mostrador BCT-F12

Spec ID: `bct-f12-blindaje-mostrador`
Capability: `mostrador-offline-sync-auth-ticket`
Strict TDD: `parcial`

## Problema

- El mostrador tiene riesgos pequenos pero criticos acumulados: apertura de turno dependiente de una consulta Firestore no permitida para vendedor, cola offline de stock vulnerable a estados `SYNCING` colgados, sincronizacion de stock offline no-base con payload inconsistente, tickets WhatsApp sin descuento manual y doble guardado de undo en carrito.

## Alcance

- Ajustar permisos de apertura de turno para vendedor sin list query de empleados.
- Recuperar ajustes de stock offline que quedaron en `SYNCING` antes de sincronizar.
- Sincronizar ajustes offline usando cantidad base y payload compatible con inventario remoto.
- Incluir descuento manual en tickets WhatsApp generados desde checkout.
- Centralizar undo del carrito en `CartManager`.
- Agregar tests de contrato/unitarios focalizados.
- Registrar pendientes en `notas.txt`.

## Fuera de Alcance

- Redisenar administracion de tienda o inventario.
- Migrar Room/SQLite o cambiar schema.
- Cambiar Gradle, version catalog, AGP, Kotlin, CI o dependencias.
- Cambiar reglas Firestore en esta fase.
- Redisenar checkout visual o caja.
- Ejecutar pruebas AVD completas si el emulador o Gradle estan ocupados por trabajo paralelo.

## Archivos Afectados Esperados

- `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/InventoryRepositoryImpl.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCase.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/CheckoutUseCase.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/CartManager.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SalesViewModelV2.kt`
- Tests relacionados en `app/src/test/java/...`
- `notas.txt`

## Riesgos

- Ventas: bajo, no cambia calculo de total ni persistencia principal de venta.
- Inventario: medio, toca sync remoto de ajustes offline; mitigado convirtiendo a unidad base.
- Offline/sync: medio, toca `SyncWorker`; mitigado recuperando estados viejos antes de leer pendientes.
- Caja/pagos: bajo, ticket agrega descuento manual sin cambiar cobro.
- UI: bajo, no toca Composables.
- Seguridad: medio, modifica decision de permiso de apertura de turno; queda limitada a `Rol.VENDEDOR` y `AccionSensible.ABRIR_TURNO`.

## Research Externo

- Web requerida: no.
- Pregunta investigada: no aplica.
- Fuentes primarias: codigo local, `AGENTS.md`, constitution y skills locales.
- Decision tomada: correcciones quirurgicas sobre owners actuales.
- Suposicion sensible a version: ninguna.

## Estrategia de Rollback

- Revertir los cambios de esta fase por archivo si una prueba critica falla.
- La correccion de tickets es compatible hacia atras por parametro default.
- La recuperacion de cola solo cambia estados stale; si falla, se puede remover la llamada inicial en `SyncWorker`.
- La centralizacion de undo se puede revertir aislando `CartManager` y `SalesViewModelV2`.

## Criterios de Exito

- [x] Vendedor puede abrir turno sin consulta `whereEqualTo("authUid")` en el path de permiso.
- [x] `SyncWorker` reinicia ajustes `SYNCING` obsoletos antes de leer pendientes.
- [x] Ajustes offline no-base se convierten a cantidad base para Firestore.
- [x] Ticket WhatsApp incluye descuento manual cuando existe.
- [x] Undo del carrito tiene un solo owner funcional.
- [x] Gradle `compileDebugKotlin` y unit tests relevantes pasan cuando no haya conflicto de procesos.

## Checkpoint Humano

El usuario pidio continuar y usar SDD. Esta propuesta queda aceptada implicitamente para documentar la fase ya iniciada y cerrar verificacion.
