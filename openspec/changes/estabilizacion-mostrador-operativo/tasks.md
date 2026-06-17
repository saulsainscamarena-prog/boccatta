# Tasks: Estabilizacion integral de mostrador

Spec ID: `BCT-F15-MOSTRADOR-STABLE`

## Reglas

- Cada fase termina en commit independiente.
- Strict TDD para checkout, sync y cancelaciones.
- No modificar la migracion multimodulo desde esta spec.
- No iniciar dos tareas Gradle en paralelo.

## Fase 0: Baseline

- [ ] 0.1 Recibir confirmacion de que la migracion termino.
- [ ] 0.2 Ejecutar pre-build check de daemons.
- [ ] 0.3 Ejecutar `:app:compileDebugKotlin`.
- [ ] 0.4 Ejecutar `assembleDebug`.
- [ ] 0.5 Instalar APK en `bocatta_tablet_api36`.

## Fase 1: Tests de contrato

- [ ] 1.1 Test de checkout online exitoso.
- [ ] 1.2 Test de checkout offline directo.
- [ ] 1.3 Test de fallo UNAVAILABLE con fallback local.
- [ ] 1.4 Test de PERMISSION_DENIED sin fallback.
- [ ] 1.5 Test de stock insuficiente sin fallback.
- [ ] 1.6 Test de venta remota existente sin doble deduccion.
- [ ] 1.7 Test de tres fallos transitorios no terminales.
- [ ] 1.8 Test de resumen integral y error de lectura.

## Fase 2: Checkout durable

- [ ] 2.1 Implementar clasificador compartido.
- [ ] 2.2 Implementar fallback con mismo `forcedVentaId`.
- [ ] 2.3 Agregar resultado operativo tipado.
- [ ] 2.4 Verificar propina, nota, descuentos y pago mixto.

## Fase 3: Sync observable

- [ ] 3.1 Implementar `SaleSyncOutcome`.
- [ ] 3.2 Reconciliar ticket remoto existente.
- [ ] 3.3 Separar transitorio de terminal.
- [ ] 3.4 Implementar `OfflineQueueSummary`.
- [ ] 3.5 Conectar resumen a `SalesViewModelV2`.
- [ ] 3.6 Actualizar top bar, cierre de caja y diagnostico WhatsApp.

## Fase 4: Cancelacion durable

- [ ] 4.1 Crear ruta comun para cancelacion.
- [ ] 4.2 Persistir item cancelado antes de eliminarlo.
- [ ] 4.3 Persistir carrito cancelado antes de limpiarlo.
- [ ] 4.4 Cubrir NIP, rol y fallo de persistencia.

## Fase 5: Refinamiento de pago

- [ ] 5.1 Centralizar mensajes operativos de checkout.
- [ ] 5.2 Corregir Exacto y denominaciones.
- [ ] 5.3 Verificar bloqueo por doble toque.
- [ ] 5.4 Verificar reset de estados entre ventas.

## Fase 6: UX administrativa

- [ ] 6.1 Capturar baseline movil/tablet.
- [ ] 6.2 Implementar hub Operacion/Catalogo/Personal/Configuracion.
- [ ] 6.3 Crear acceso directo Actualizar stock.
- [ ] 6.4 Mantener produccion, ajustes, alertas y reportes accesibles.
- [ ] 6.5 Validar contraste, targets y texto sin desbordes.

## Fase 7: Verificacion

- [ ] 7.1 Ejecutar tests unitarios por modulo.
- [ ] 7.2 Ejecutar tests instrumentados SQLite/WorkManager.
- [ ] 7.3 Ejecutar Compose UI tests.
- [ ] 7.4 Ejecutar diez flujos AVD.
- [ ] 7.5 Ejecutar `git diff --check`.
- [ ] 7.6 Ejecutar `assembleDebug`.
- [ ] 7.7 Completar `review.md` y `verification.md`.
- [ ] 7.8 Archivar specs sustituidas o completadas.

## Checklist Critico

- [ ] Checkout protegido contra doble toque.
- [ ] Venta offline conserva folio, monto, items y estado.
- [ ] Deduccion de inventario validada online/offline.
- [ ] Errores visibles sin detalles internos.
- [ ] Cancelaciones auditables.
- [ ] Pendientes y fallos visibles.
- [ ] No se agregaron dependencias.
- [ ] No se tocaron archivos de migracion.
