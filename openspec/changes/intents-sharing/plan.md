# Plan Tecnico: Implementación de Unified Sharing

Spec ID: `intents-sharing`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Revisión de las 5 pantallas principales identificadas en la fase de auditoría.

## Punto de Entrada Real

- Dominio/use case: El helper vivirá en el layer `core` (e.g. `core/utils/ShareUtils.kt`), accesible estáticamente o inyectado para cualquier capa de UI o ViewModel que requiera interactuar con el contexto de la Activity para lanzar Intents.
- UI: Llamadas refactorizadas desde los botones onClick de Compose.

## Archivos Probables

- `app/src/main/java/com/bocatta/pos/core/ShareUtils.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/DialogosVentas.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/AdminScreen.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/TabSueldos.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/caja/CierreCajaScreen.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/reportes/ReportScreen.kt`

## Estrategia

1. Crear un utilitario `ShareUtils.kt` con función `shareTextWhatsAppFallback(context: Context, text: String, subject: String?, logContext: String)`.
2. Refactorizar la función `compartirNomina` en `TabSueldos.kt` para usar esta función centralizada.
3. Refactorizar `AdminScreen.kt` (Diagnóstico) y `DialogosVentas.kt` (Ticket) para reemplazar sus bloques try/catch manuales.
4. Refactorizar `CierreCajaScreen.kt` y `ReportScreen.kt` para que adopten el mismo estándar.
5. Ejecutar la app localmente y verificar que ninguna clase quede con imports rotos.

## Contratos a Revisar

- Android Context: Verificar que las funciones reciban un Context de compose `LocalContext.current` puro y sin fugas de memoria.

## Impacto Offline

- Venta online: Sin impacto.
- Venta offline: Sin impacto.

## Impacto UI

- Eventos consumibles: Las llamadas a Intents bloquean momentáneamente la UI mientras se abre el resolver del sistema. Manejar el click adecuadamente o deshabilitar botón mientras lanza si es necesario.

## Plan de Pruebas

- Manual: Compartir ticket y verificar que salte WhatsApp o Chooser. 
- Manual: Compartir nómina, cierre y reportes de la misma forma.
- Manual: Desinstalar WhatsApp (o en un emulador que no lo tenga) y verificar que nunca haya crash.
- Gradle: `.\gradlew.bat compileDebugKotlin` para asegurar compatibilidad y sintaxis pura.

## Criterio para No Continuar

- Si el PO requiere que los reportes de "Cierre de Caja" SIEMPRE den a elegir en Chooser, habrá que agregar un flag a la función de share (e.g. `forceChooser: Boolean = false`). Esto debe aclararse antes de iniciar.
