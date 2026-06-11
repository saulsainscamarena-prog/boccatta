# Design: Extract NavHost from MainActivity

## Technical Approach
Extract the navigation graph into a separate composable `AppNavGraph` to isolate navigation concerns from `MainActivity`. `MainActivity` will only handle activity lifecycle, view model initialization, and host `AppNavGraph`.

## Architecture Decisions

### Decision: Separate navigation graph file
**Choice**: Create `AppNavGraph.kt` in `com.bocatta.pos.navigation`.
**Alternatives considered**: Keep NavHost in `MainActivity`; split into multiple smaller graphs.
**Rationale**: Improves single‑responsibility, reduces `MainActivity` size, and keeps navigation definitions centralized.

### Decision: Parameterize navigation callbacks
**Choice**: Pass all 19 navigation lambdas as parameters to `AppNavGraph`.
**Alternatives considered**: Use a shared navigation manager or retrieve view models inside the graph.
**Rationale**: Keeps the API explicit and mirrors the current `MainActivity` contract, minimizing behavioural changes.

## Component Design

### `AppNavGraph` signature
```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authVm: SessionViewModel,
    onLoginExitoso: (String) -> Unit,
    onIniciarTurno: () -> Unit,
    onUnirseTurno: () -> Unit,
    onAdministrarTienda: () -> Unit,
    onLogout: () -> Unit,
    onAperturaCompleta: () -> Unit,
    onVerInventario: () -> Unit,
    onVerReportes: () -> Unit,
    onVerGastos: () -> Unit,
    onVerAdmin: () -> Unit,
    onVerCaja: () -> Unit,
    onVerDevoluciones: () -> Unit,
    onVerActividad: () -> Unit,
    // ...remaining callbacks for the other routes
) {
    NavHost(
        navController = navController,
        startDestination = if (authVm.estaLogueado) Routes.Turnos else Routes.Login
    ) {
        // composable definitions (see examples below)
    }
}
```

### Conditional `startDestination`
The `startDestination` is computed inside `AppNavGraph` using `authVm.estaLogueado`. This mirrors the current logic in `MainActivity` and avoids duplication.

### Route wiring examples
```kotlin
composable<Routes.Login> {
    LoginScreen(vm = authVm, onLoginExitoso = onLoginExitoso)
}

composable<Routes.Turnos> {
    // view‑model retrieval stays the same
    TurnosScreen(
        sessionVm = sessionVm,
        cajaVm = cajaVm,
        inventarioVm = inventoryVm,
        participantes = participantes,
        jornadaActiva = jornadaActiva,
        onIniciarTurno = onIniciarTurno,
        onUnirseTurno = onUnirseTurno,
        onAdministrarTienda = onAdministrarTienda,
        onLogout = onLogout
    )
}

composable<Routes.Ventas> {
    SalesScreen(
        vmV2 = salesVmV2,
        cajaVm = cajaVm,
        heldOrderVm = heldOrderVm,
        session = sessionVm,
        onVerInventario = onVerInventario,
        onVerReportes = onVerReportes,
        onVerGastos = onVerGastos,
        onVerAdmin = onVerAdmin,
        onVerCaja = onVerCaja,
        onVerDevoluciones = onVerDevoluciones,
        onVerActividad = onVerActividad,
        onLogout = onLogout
    )
}
```
*The remaining 16 routes are copied verbatim, each receiving the appropriate callback parameter.*

## Data Flow
```
AuthVm (SessionViewModel) → AppNavGraph → determines startDestination

AppNavGraph → composable routes → callbacks → NavController.navigate()
```
State such as `sessionVm`, `cajaVm`, and `inventoryVm` continues to be obtained via Koin inside each composable, identical to the original implementation.

## File Changes
| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/com/bocatta/pos/navigation/AppNavGraph.kt` | Create | Contains the `AppNavGraph` composable and all route definitions. |
| `app/src/main/java/com/bocatta/pos/MainActivity.kt` | Modify | Removes the NavHost block, adds import for `AppNavGraph`, creates `navController` and calls `AppNavGraph` passing view models and lambdas. |
| `openspec/changes/extract-navhost-main-activity/design.md` | Create | This design document. |

## Interfaces / Contracts
- No new interfaces are introduced. Existing sealed class `Routes` remains the source of truth for route identifiers.
- Callback signatures are defined in `AppNavGraph` parameters and match the lambdas previously embedded in `MainActivity`.

## Testing Strategy
| Layer | What to Test | Approach |
|-------|--------------|----------|
| Compile | All Kotlin sources compile after extraction | Run `./gradlew assembleDebug`. |
| Unit | `AppNavGraph` startDestination logic | Verify `startDestination` resolves to `Routes.Turnos` when `authVm.estaLogueado` is true and to `Routes.Login` otherwise (use Robolectric or Compose testing). |
| Integration / UI | Navigation between screens | Execute existing UI tests (if any) and add a small Compose test that clicks a navigation button and asserts the destination route. |

## Migration / Rollout
No data migration needed. The change is purely structural; the app behaviour remains identical.

## Open Questions
- None.

## Next Step
Ready for task generation (`sdd-tasks`).