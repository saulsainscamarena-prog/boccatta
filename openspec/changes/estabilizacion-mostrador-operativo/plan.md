# Plan Tecnico: Estabilizacion integral de mostrador

Spec ID: `BCT-F15-MOSTRADOR-STABLE`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Esta spec completa.
- Archivos propietarios del flujo antes de cada fase.

## Punto de Entrada Real

- UI: `SalesScreen`, `DialogosVentas`, `AdminScreen`.
- ViewModel: `SalesViewModelV2`.
- Dominio/use case: `CheckoutUseCase`.
- Repositorio/data: `FirebaseSalesRepositoryV2`, `OfflineManager`,
  `OfflineDatabase`.
- DI: `DataModule`, `VentasModule`.
- Tests: nuevos tests por modulo; harness AVD/Maestro existente.

## Fases

### Fase 0: Baseline compilable

Owner: agente de migracion.

1. Resolver `CarritoPanelV2.kt:169` y cualquier error posterior.
2. Ejecutar `:app:compileDebugKotlin` y `assembleDebug`.
3. Congelar cambios de Gradle/navegacion durante las fases 1-5.

Gate: no tocar checkout hasta tener APK instalable o un commit base estable.

### Fase 1: Contratos y pruebas antes de codigo

1. Crear fakes de `SalesRepository`, almacenamiento y clasificador.
2. Cubrir checkout online, offline, transitorio, permisos y stock.
3. Cubrir sync `AlreadyExists`, transitorio repetido y permanente.
4. Cubrir resumen completo y error de lectura.

Gate: tests deben fallar por las conductas faltantes, no por DI.

### Fase 2: Durabilidad de checkout

1. Extraer clasificador compartido de errores.
2. Mantener remoto primero.
3. Ante fallo transitorio, guardar local con `forcedVentaId`.
4. Devolver `CheckoutResult` con origen real y mensaje operativo.
5. No ejecutar fallback ante errores permanentes o de negocio.

Gate: pruebas de ID, monto, items, propina, nota, descuentos y folio pasan.

### Fase 3: Sync e indicador operativo

1. Reconciliar venta remota existente antes de marcar sincronizada.
2. Mantener errores transitorios pendientes con backoff.
3. Reservar estado critico para datos/permisos permanentes.
4. Implementar `OfflineQueueSummary`.
5. Mostrar pendientes y fallos por separado en mostrador/cierre.
6. Actualizar diagnostico compartible de WhatsApp.

Gate: reinicio de proceso no pierde cola ni estado.

### Fase 4: Cancelaciones auditables

1. Centralizar registro de cancelacion de item y carrito.
2. Guardar operacion durable antes de modificar el carrito.
3. Incluir motivo, usuario, sucursal, items, total y alcance.
4. Limpiar carrito solo tras persistencia local exitosa.
5. Sincronizar por la ruta existente de operaciones.

Gate: cancelacion offline aparece pendiente y luego en Firestore.

### Fase 5: Pago y mensajes

1. Mapear errores tecnicos a mensajes de cajero.
2. Mantener excepcion completa en Timber/diagnostico.
3. Corregir chips de efectivo: reemplazo consistente.
4. Reservar etiqueta Exacto para el total decimal real.
5. Verificar que pago mixto, propina y nota se reinicien.

Gate: suite Compose y flujo AVD de pagos pasan.

### Fase 6: Administracion e inventario

1. Convertir dashboard en hub de tareas, no catalogo de opciones.
2. Acciones principales: Actualizar stock, Registrar produccion, Ajustes y
   Alertas.
3. Separar Operacion, Catalogo, Personal y Configuracion.
4. Mantener subnavegacion horizontal compacta en movil y panel de apoyo en
   tablet.
5. Usar solo tokens `MaterialTheme`; no adoptar Styles experimental.

Gate: actualizar stock en maximo dos acciones en movil y tablet.

### Fase 7: Certificacion AVD

1. Venta online exacta.
2. Pago mixto.
3. Propina y nota.
4. Venta offline.
5. Corte de red durante confirmacion.
6. Reconexion y sync.
7. Reinicio con cola pendiente.
8. Cancelacion offline.
9. Venta fallida permanente visible.
10. Actualizacion de stock desde administracion.

Gate final: cero ventas perdidas, duplicadas o invisibles.

## Contratos a Revisar

- Modelos: resultados de checkout, sync y resumen.
- Interfaces: almacenamiento offline y cola de stock.
- Koin: solo al conectar nuevos contratos.
- Firestore/Room: ID, ticket y estados.
- WorkManager/sync: backoff y estados terminales.

## Impacto Offline

- Venta online: conserva ruta actual.
- Venta offline: durable y confirmada.
- Reconexion: idempotente y reconciliada.
- Deduccion de inventario: formulas sin cambios.
- Idempotencia/reintentos: `forcedVentaId` como identidad.

## Impacto UI

- Movil: controles compactos y navegacion administrativa de una fila.
- Tablet: mostrador sin scroll innecesario y hub operativo.
- Estados de carga/error: bloqueo de doble toque y mensajes accionables.
- Eventos consumibles: error/success limpiados tras mostrarse.
- Accesibilidad: targets de 48 dp y semantica en iconos.

## Plan de Pruebas

- Unit: checkout, clasificador, sync, resumen y cancelacion.
- Instrumented: SQLite atomico, folios y reconciliacion.
- Compose UI: pago exacto, chips, errores e indicador offline.
- Maestro/manual: diez flujos de certificacion.
- Gradle: por modulo, luego `testDebugUnitTest`, `assembleDebug`.

## Criterio para No Continuar

- Build base rojo por migracion.
- Gradle/Kotlin ejecutandose en otra sesion.
- Cambio requiere modificar formulas de inventario sin tests.
- No puede demostrarse idempotencia para el mismo `forcedVentaId`.
- El AVD usa una APK anterior a la fuente verificada.
