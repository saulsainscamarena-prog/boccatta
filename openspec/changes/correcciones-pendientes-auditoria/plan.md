# Plan: Correcciones Pendientes de Auditoría

## Estrategia
1. Reemplazar tests frágiles con tests de comportamiento real (reflection/integración)
2. Reemplazar CountDownLatch con runTest en test instrumentado
3. Extraer strings a resources por pantalla, empezando por las de mayor impacto

## Archivos Probables
- `app/src/test/java/com/bocatta/pos/security/OfflineOperationsContractTest.kt`
- `app/src/test/java/com/bocatta/pos/security/PinAuthorizationRegressionTest.kt`
- `app/src/test/java/com/bocatta/pos/security/FirestoreRulesContractTest.kt`
- `app/src/androidTest/.../CajaViewModelContingencyInstrumentedTest.kt`
- `app/src/main/res/values/strings.xml`
- Screens: LoginScreen, SalesScreen, CarritoPanelV2, CierreCajaScreen, AdminScreen
