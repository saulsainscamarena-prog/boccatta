# Proposal: Estabilizacion integral de mostrador

Spec ID: `BCT-F15-MOSTRADOR-STABLE`
Capability: `counter_operation_stability`
Strict TDD: `true`

## Problema

Bocatta POS completa ventas online, pero no puede considerarse listo para
mostrador porque:

- La compilacion integral no esta verde por cambios concurrentes.
- Una venta puede no persistirse si Firestore falla despues de detectar red.
- El contador de pendientes no representa toda la deuda offline.
- Los errores tecnicos pueden mostrarse al cajero.
- La cancelacion total descarta su motivo pese a anunciar revision.
- Los fallos transitorios terminan como ventas criticas tras tres intentos.
- El flujo administrativo de stock sigue disperso.

## Justificacion del Governance Gate

Existen 25 cambios SDD abiertos. Esta propuesta ignora el soft gate porque no
introduce una feature independiente: centraliza los defectos P0/P1 que impiden
operar el mostrador y sustituye la ejecucion fragmentada por un solo orden de
estabilizacion. No absorbe la migracion multimodulo ni Play Store.

## Alcance

- Recuperar un baseline compilable coordinado con la migracion.
- Hacer durable el checkout ante fallos remotos transitorios.
- Reconciliar reintentos por ID sin doble venta ni doble inventario.
- Exponer resumen completo de pendientes y fallos.
- Hacer auditable la cancelacion total.
- Mostrar errores operativos y conservar detalles en logs.
- Corregir semantica de efectivo rapido.
- Simplificar el acceso administrativo a stock como fase final.
- Agregar pruebas unitarias, SQLite, WorkManager, Compose y AVD.

## Fuera de Alcance

- Resolver o redisenar la migracion multimodulo.
- Actualizar AGP, Kotlin, Compose, Koin o Gradle.
- Adoptar la API experimental Compose Styles.
- Play Store, firma, release o marketing.
- Reescribir todo el almacenamiento a Room.
- Cambiar formulas de recetas o deducciones de ingredientes.

## Archivos Afectados Esperados

- `core/data/.../CheckoutUseCase.kt`
- `core/data/.../OfflineManager.kt`
- `core/data/.../OfflineDatabase.kt`
- `core/data/.../OfflineStorage.kt`
- `core/data/.../SyncWorker.kt`
- `core/data/.../SyncScheduler.kt`
- `feature/ventas/.../SalesViewModelV2.kt`
- `feature/ventas/.../SalesScreen.kt`
- `feature/ventas/.../SalesScreenSections.kt`
- `feature/ventas/.../DialogosVentas.kt`
- `feature/admin/.../AdminScreen.kt`
- Pruebas de los modulos propietarios.

## Riesgos

- Ventas: fallback incorrecto podria aceptar una venta rechazada por negocio.
- Inventario: reintento ambiguo podria duplicar una deduccion remota.
- Offline/sync: cambios de estados podrian dejar registros sin recuperar.
- Caja/pagos: una confirmacion duplicada podria cobrar dos veces.
- UI: indicadores incorrectos pueden ocultar deuda offline.
- Seguridad: una cancelacion sin identidad valida no debe registrarse.

## Research Externo

- Web requerida: no.
- Pregunta investigada: comportamiento y ownership local.
- Fuentes primarias: codigo, AVD, AGENTS, constitution y skills locales.
- Decision tomada: remoto primero con fallback clasificado e idempotente.
- Suposicion sensible a version: ninguna.

## Estrategia de Rollback

- Un commit por fase.
- No mezclar UX administrativa con checkout/sync.
- Mantener contratos anteriores hasta que sus pruebas nuevas pasen.
- Si falla la reconciliacion de tickets, conservar venta local pendiente y no
  marcarla sincronizada.

## Criterios de Exito

- [ ] APK debug compila e instala.
- [ ] Ningun fallo transitorio de red deja una venta sin persistencia local.
- [ ] Errores de permisos/stock no generan ventas offline.
- [ ] Reintentos con el mismo ID no duplican venta ni stock.
- [ ] El cajero ve pendientes, fallos y mensajes accionables.
- [ ] Cancelacion total genera un registro durable.
- [ ] Flujos online, offline y reconexion pasan en AVD.

## Checkpoint Humano

El pedido actual autoriza producir la spec y el plan completo. La aplicacion de
codigo comienza solo cuando el baseline de migracion vuelva a compilar.
