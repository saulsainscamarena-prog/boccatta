# Verification

## Static

- `git diff --check` passed for touched files in this change.

## Gradle

- First `compileDebugKotlin` failed because Kotlin daemon/incremental state broke:
  `NoSuchFileException kotlin-backups... -> app.kotlin_module`, followed by false
  unresolved references to local packages.
- Stopped Gradle/Kotlin daemon state and retried with:
  `.\gradlew.bat compileDebugKotlin --no-daemon --no-parallel --max-workers=1 "-Pkotlin.compiler.execution.strategy=in-process" "-Pkotlin.incremental=false"`
- Result: build successful.
- `.\gradlew.bat assembleDebug --no-daemon --no-parallel --max-workers=1 "-Pkotlin.compiler.execution.strategy=in-process" "-Pkotlin.incremental=false"`
- Result: build successful.

## AVD Startup

- Device: `bocatta_tablet_api36`, serial `emulator-5554`.
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk` succeeded.
- Launch: `adb shell am start -W -n com.bocatta.pos/.MainActivity`
- Result: `Status: ok`, `LaunchState: COLD`, `TotalTime: 8732`, `WaitTime: 8747`.
- UI reached `Turnos` with active Metepec shift.

## AVD Sale Flow

- Joined active shift.
- Added `Carlota de Limón` to cart.
- Opened `Pago final`.
- Used `Cobrar efectivo exacto`.
- Result: success dialog with ticket `MT0706-005` in `METEPEC`.
- Logcat showed:
  - `stock_validation_ok sucursal=metepec`
  - `online_save_start`
  - `sale_finish_success | ticket=MT0706-005`
- Local pending tables after sale:
  - `ventas_pendientes = 0`
  - `operaciones_pendientes = 0`
  - `stock_adjustments = 0`

## Residual Risk

- Startup still has debug jank: cold start around 8.7 seconds and Compose runtime
  compilation logs for `SalesScreen`.
- WorkManager still initializes because there are existing unfinished jobs; this
  change only removed debug log-cleanup scheduling, not sync infrastructure.
