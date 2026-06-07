# Design

## Owner

`DialogosVentas.kt` owns both `PagoSheetV2` and `PantallaLockInactividad`.

## Implementation Decision

Use `Color.Black.copy(alpha = 0.72f)` for the lock background instead of theme `scrim`, and keep the PIN card using Material surface tokens.

Do not do the full `PagoSheetV2` layout rewrite in this microchange. A no-scroll tablet payment sheet is still desired, but it should be implemented as a separate, testable checkout UI task.
