# Design: Caja Retiro y Arqueo

## Technical Approach

The change follows the existing MVVM + Repository pattern. We add:
1. New model `RetiroParcialV2` in `domain/model/ModelsV2.kt`.
2. New fields to `TurnoCajaV2`: `denominacionesContadas: Map<String, Int>` and `cajaId: String` (default "1").
3. New Firestore sub‑collection constant for retiros.
4. ViewModel methods in `CajaViewModel` for retiro creation and denomination management.
5. Wire `CuadreCajaManager` into the existing `cerrarTurno` flow, replacing the inline calculations.

## Architecture Decisions

### Decision: Retiro storage as Firestore sub‑collection
**Choice**: Store each partial cash withdrawal as a document under `TURNOS_CAJA/{turnoId}/RETIROS`.
**Alternatives considered**: Top‑level collection `RETIROS` or embedding an array in `TurnoCajaV2`.
**Rationale**: Sub‑collection scales for many retiros, allows per‑turno queries, and follows Firebase best practices for 1‑N relationships.

### Decision: `denominacionesContadas` representation
**Choice**: Use a `Map<String, Int>` where keys are denomination values as strings (e.g., "1000", "500").
**Alternatives considered**: List of objects `{denomination:Int, quantity:Int}`.
**Rationale**: Map is directly serialisable by Firestore, simpler to compute totals, and matches existing use of maps for other aggregated data.

### Decision: Atomic update for retiros
**Choice**: Perform a Firestore transaction that creates the retiro document **and** increments `totalGastosTurno` atomically.
**Alternatives considered**: Separate writes with client‑side compensation.
**Rationale**: Guarantees consistency even under concurrent operations and aligns with existing transaction pattern used for `cerrarTurno`.

### Decision: Integrate `CuadreCajaManager`
**Choice**: Replace inline `diferenciaCaja` / `cadraCaja` logic with a single call to `CuadreCajaManager.calcularCuadre`.
**Alternatives considered**: Keep inline calculations for backward compatibility.
**Rationale**: Centralises business rule, reduces duplication, and makes future adjustments easier.

### Decision: PIN validation for retiros
**Choice**: Reuse `authManager.verificarPermiso` with a new sensitive action `GASTAR_CAJA`.
**Alternatives considered**: Separate auth flow.
**Rationale**: Keeps authentication consistent across the app and leverages existing audit logging.

### Decision: `cajaId` default
**Choice**: Add `cajaId` field with default "1" to support future multi‑caja scenarios.
**Alternatives considered**: No field until multi‑caja is required.
**Rationale**: Minimal impact now, future‑proofs the model without breaking existing documents.

## Data Flow

```
REGISTRAR RETIRO:
User → UI (CierreCajaScreen) → ViewModel.registrarRetiroParcial()
  → authManager.verificarPermiso(PIN)
  → Firestore transaction:
       TURNOS_CAJA/{turnoId}/RETIROS/ → .add(retiro)
       TURNOS_CAJA/{turnoId} → .update(totalGastosTurno: FieldValue.increment(monto))
  → authManager.registrarAuditoria()
  → Update local totalGastosSistema

ARQUEO POR DENOMINACIÓN:
User → UI (DenominationGrid) → ViewModel.actualizarDenominaciones(map)
  → Local state: denominacionesInput: SnapshotStateMap<String, String>
  → Computed: efectivoContado = denominacionesInput.entries
      .sumOf { (denom, qty) → (denom.toInt() * qty.toInt()) }
  → On close: stored in TurnoCajaV2.denominacionesContadas

CIERRE CON CUADRE MANAGER:
ViewModel.cerrarTurno()
  → resultado = CuadreCajaManager.calcularCuadre(
       totalEfectivoSistema, totalGastosSistema, fondoInicial,
       efectivoContado, toleranciaEfectivo, toleranciaTarjeta)
  → if resultado.esCorrecto OR user is admin → proceed
  → Firestore write of all fields including denominacionesContadas and cajaId
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/com/bocatta/pos/domain/model/ModelsV2.kt` | Modify | Add `RetiroParcialV2` data class, add `denominacionesContadas: Map<String, Int>` and `cajaId: String` to `TurnoCajaV2` |
| `app/src/main/java/com/bocatta/pos/core/constants/FirestoreCollections.kt` | Modify | Add `RETIROS = "retiros"` constant |
| `app/src/main/java/com/bocatta/pos/presentation/viewmodel/CajaViewModel.kt` | Modify | Add `retiroList`, `registrarRetiroParcial()`, `actualizarDenominaciones()`, refactor `cerrarTurno()` to use `CuadreCajaManager`, wire denominations into `efectivoContado` |
| `app/src/main/java/com/bocatta/pos/presentation/ui/screens/caja/CierreCajaScreen.kt` | Modify | Add denomination grid UI, add "Registrar Retiro" dialog/button |
| `app/src/main/java/com/bocatta/pos/presentation/ui/components/DialogosVentas.kt` | Create | New composable for denomination entry (or extend existing dialog) |
| `app/src/main/java/com/bocatta/pos/domain/CuadreCajaManager.kt` | No change | Existing manager will be used |

## Interfaces / Contracts

```kotlin
// New model for partial cash withdrawal
data class RetiroParcialV2(
    val id: String = "",
    val monto: Double = 0.0,
    val motivo: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val usuarioId: String = ""
)

// Updated TurnoCajaV2
data class TurnoCajaV2(
    // existing fields …
    val cajaId: String = "1",
    val denominacionesContadas: Map<String, Int> = emptyMap(),
    // new field for future use – optional
    // other existing fields unchanged
)
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Unit | `RetiroParcialV2` construction & defaults | Simple object creation test |
| Unit | `CuadreCajaManager.calcularCuadre` with denomination totals | Parameterised tests covering in‑tolerance & out‑of‑tolerance |
| Unit | Denomination sum calculation in ViewModel | Mock `denominacionesInput` map and verify `efectivoContado` |
| Integration | `CajaViewModel.registrarRetiroParcial` transaction | Mock Firestore SDK, verify `add` and `update` calls are within a transaction |
| Unit | PIN validation flow for retiros | Mock `authManager.verificarPermiso` for admin vs cashier |

## Migration / Rollout

No data migration is required. Existing `TurnoCajaV2` documents will receive the new fields lazily when the turn is closed next time. `cajaId` defaults to "1". `denominacionesContadas` defaults to an empty map, preserving backward compatibility.

## Open Questions
- Should retiros also support a denomination breakdown? (deferred)
- Should `denominacionesContadas` be mandatory in every closed turn, or optional with default empty map? (chosen optional for backward compatibility)
