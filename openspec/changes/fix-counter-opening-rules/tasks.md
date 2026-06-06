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

- [ ] 4. Ejecutar flujo de venta smoke.
  - Archivos: ninguno
  - Verificacion: agregar producto, cobrar, confirmar pago o capturar nuevo error exacto.
