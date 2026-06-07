# Tasks: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`

## Tareas

- [x] 1. Confirmar owner actual del comportamiento.
  - Archivos: `AperturaDiaScreen.kt`, `AperturaViewModelV2.kt`, `StockAllocationRepository.kt`, `CajaViewModel.kt`, `firestore.rules`
  - Verificacion: AVD/logcat con `PERMISSION_DENIED` exacto.

- [x] 2. Ajustar reglas remotas de apertura.
  - Archivos: `firestore.rules`
  - Verificacion: `firebase deploy --only firestore:rules`

- [x] 3. Repetir apertura real en AVD.
  - Archivos: ninguno
  - Verificacion: llegar a `BOCATTA POS` / mostrador.

- [x] 4. Ejecutar flujo de venta smoke.
  - Archivos: ninguno
  - Verificacion: se registro venta `MT0606-001` en `v2_ventas/BC27S0V2pGCSdOJYKue0`; despues se capturo y corrigio divergencia de stock `currentQty`.

- [x] 5. Corregir compatibilidad de stock para checkout/sync.
  - Archivos: `InventoryRepositoryImpl.kt`, `FirebaseSalesRepositoryV2.kt`, `SyncWorker.kt`, `InventoryItem.kt`
  - Verificacion: `compileDebugKotlin`, `assembleDebug`, AVD con stock remoto divergente ya no falla por `carlota_unidad`.

- [x] 6. Repetir flujo completo post-fix sin interrupcion de `adb`.
  - Archivos: ninguno
  - Verificacion: agregar producto simple, cobrar efectivo exacto, confirmar, validar nuevo documento de venta y decremento de stock.
  - Bloqueo actual: conflicto externo entre `adb` 1.0.40 en `C:\Windows\adb.exe` y `adb` 1.0.41 del SDK reinicia el servidor durante la automatizacion.
