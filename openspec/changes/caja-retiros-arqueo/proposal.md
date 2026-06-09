# Proposal: Caja — Retiros Parciales y Arqueo por Denominación

Spec ID: `caja-retiros-arqueo`
Capability: `caja-turnos`
Strict TDD: `true`

## Problema

La caja actual solo tiene apertura → ventas → cierre con un monto global de efectivo. No hay forma de:
1. Registrar retiros de efectivo durante el turno (pagos a proveedores, gastos menores, retiros del dueño)
2. Contar billetes/monedas por denominación al hacer el arqueo
3. Auditar movimientos de efectivo intra-turno

## Alcance

1. **RetiroParcialV2** — modelo, subcolección Firestore RETIROS, creación atómica con actualización de totalGastosTurno
2. **Denominaciones** — campo `denominacionesContadas: Map<String, Int>` en TurnoCajaV2, cómputo automático de efectivoContado
3. **Refactor Cuadre** — integrar CuadreCajaManager existente en CajaViewModel, reemplazar lógica duplicada

## Fuera de Alcance

- Múltiples cajas por sucursal
- Offline contingency para retiros
- Integración con terminales de pago externos
- Facturación fiscal / CFDI

## Archivos Afectados Esperados

- `domain/model/ModelsV2.kt` — +RetiroParcialV2, +denominacionesContadas en TurnoCajaV2
- `domain/CuadreCajaManager.kt` — sin cambios (ya existe)
- `core/constants/FirestoreCollections.kt` — +RETIROS
- `data/repository/*.kt` — nuevo CajaRepository o métodos en repositorio existente
- `presentation/viewmodel/CajaViewModel.kt` — +registrarRetiroParcial, +actualizarDenominaciones, refactor cuadre
- `presentation/ui/screens/caja/CierreCajaScreen.kt` — UI de denominaciones y retiros
- `domain/model/TransactionV2.kt` — posible extensión para tracking

## Riesgos

- Ventas: N/A (no toca checkout)
- Inventario: N/A (no toca stock)
- Offline/sync: Retiros offline no se implementan en este cambio — riesgo bajo
- Caja/pagos: **ALTO** — cambios en lógica de cuadre. Proteger con tests y validación por PIN
- UI: Medio — pantalla de denominaciones nueva
- Seguridad: Retiros deben requerir autorización (admin PIN)

## Governance Gate Override

Justificación: Este cambio inicia una campaña sistemática para completar los módulos core del POS. Los cambios SDD abiertos existentes son borradores incompletos de sesiones anteriores. Este cambio sigue el workflow SDD del constitution.md de principio a fin y se archivará correctamente.

## Research Externo

- Web requerida: no
- Decision tomada: Se implementa con subcolección Firestore para escalabilidad

## Estrategia de Rollback

1. RetiroParcialV2: eliminar subcolección RETIROS de cada turno, revertir campo array en TurnoCajaV2
2. Denominaciones: eliminar campo denominacionesContadas de TurnoCajaV2
3. Refactor Cuadre: revertir a lógica inline anterior

## Criterios de Exito

- [ ] Administrador puede registrar un retiro parcial de efectivo durante el turno
- [ ] Al cerrar turno, puede contar billetes/monedas por denominación
- [ ] El total de efectivo contado se calcula automáticamente desde las denominaciones
- [ ] CuadreCajaManager se usa como fuente única de verdad para diferencia/tolerancia
- [ ] Todos los retiros quedan auditados en Firestore
- [ ] Tests unitarios pasan para toda la lógica nueva
- [ ] compileDebugKotlin pasa sin errores
