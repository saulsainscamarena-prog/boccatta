# Research: Hardware SDKs and Memory Leaks Audit

## Context
The user requested an audit of the Bocatta POS codebase to identify:
1. Hardware SDK integrations (Printers, Barcode Scanners, Scales, etc.) such as Sunmi, Epson, or Zebra.
2. Memory leaks related to hardware, such as lingering `Context` instances, `Activity` references, sockets, or listeners not released in `onCleared()` or lifecycle callbacks.

## Findings

### 1. Hardware SDKs
An extensive code search across the entire project (`app/src/main/java/...`) for terms related to hardware integrations (`printer`, `scanner`, `scale`, `usb`, `bluetooth`, `epson`, `zebra`, `sunmi`, `Socket`, `hardware`, `UsbManager`, `BroadcastReceiver`) yielded **0 results**.
- There are currently **no** printer integrations (thermal printers).
- There are **no** barcode scanner SDKs (or they operate via keyboard wedge emulation which doesn't require explicit code integration).
- There are **no** scale integrations using USB/Serial/Bluetooth.

### 2. Memory Leaks & Listeners
An analysis of the `ViewModels` and core application classes (`MainActivity`, `BocattaApp`, `BaseAndroidViewModel`) was conducted to find lingering references.
- **Context/Activity leaks**: ViewModels that require context inherit from `AndroidViewModel` (or `BaseAndroidViewModel`) and use the `Application` context, which is safe and does not cause activity leaks. No `Activity` references are passed to ViewModels.
- **Firestore Listeners**: The application uses several Firestore `ListenerRegistration` objects for real-time synchronization. Examples include:
  - `ReportViewModelV2` uses `salesListener`, `sales7DaysListener`, `expenseListener`, `mermaListener`, and `stockAlertListener`.
  - `SalesViewModelV2` uses `menuListener` and `stockListener`.
  - `SessionViewModel` uses `listenerDevoluciones`.
  - `SolicitudViewModel` uses `listener`.
- **Validation**: Every single `ListenerRegistration` identified in the ViewModels is properly cleared via `listener?.remove()` within the `onCleared()` callback.
- **Network Callbacks**: `SessionViewModel` registers a `ConnectivityManager.NetworkCallback` which is correctly unregistered in its `onCleared()` method.

## Conclusion
The codebase is currently free of direct hardware SDK integrations. Real-time listeners and callbacks used for network and database synchronization are properly managed and released in standard Android lifecycle methods (`onCleared()`), preventing the requested class of memory leaks.
