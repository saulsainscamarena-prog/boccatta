# Plan de Pruebas Integrales en Emulador - POS Bocatta

Este plan detalla las acciones para validar la estabilidad, funcionalidad y rendimiento de la aplicación en un entorno de emulador, cubriendo desde pruebas unitarias instrumentadas hasta flujos de usuario completos (E2E).

## 1. Verificación de Entorno
- [ ] Validar conexión con el emulador (ADB).
- [ ] Compilar el proyecto para asegurar que no hay errores de build pendientes.

## 2. Pruebas Automatizadas (Instrumented Tests)
Se ejecutarán los siguientes test suites críticos:
- **Flujo de Ventas (E2E):** `SalesE2ETest.kt` para validar el proceso de cobro completo.
- **Persistencia Offline:** `OfflineDatabaseFolioInstrumentedTest.kt` para asegurar que los folios y ventas locales sean íntegros.
- **Interfaz de Usuario:** `CarritoPanelV2Test.kt` y `SalesScreenTest.kt`.
- **Inyección de Dependencias:** `KoinAndroidModuleTest.kt`.

## 3. Pruebas Manuales y Verificación Visual (Smoke Test)
- [ ] Desplegar la aplicación en el emulador.
- [ ] **Módulo de Ventas:** Agregar productos, aplicar descuentos y simular un cobro.
- [ ] **Módulo de Inventario:** Verificar la carga de stock.
- [ ] **Administración:** Navegar por las pestañas de configuración.
- [ ] Capturar capturas de pantalla para auditoría visual.

## 4. Validación de Robustez
- [ ] Simular desconexión de red durante una venta para validar el comportamiento offline.
- [ ] Verificar que no existan crashes al navegar rápidamente entre pantallas.

---
<!--
RECOMENDACIÓN:
Se recomienda cerrar procesos pesados en el PC para que el emulador y Gradle tengan suficiente RAM (16GB disponibles).
-->
