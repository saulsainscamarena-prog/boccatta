# Research: Android Intents and Sharing Audit

Spec ID: `intents-sharing`
Capability: `Android Intent Action Send`
Fecha: `2026-06-06`

## Brief Humano

Auditar los mecanismos de intents y sharing en Bocatta POS. Revisar tickets compatibles con WhatsApp, reportes de nómina, fallbacks de ACTION_SEND y exportaciones de texto seguro. No modificar código, solo generar la SDD con findings.

## Web Research Gate

- Research question: N/A
- Source category from `openspec/source-catalog.yaml`: N/A
- Why local context is insufficient: Contexto local es suficiente.
- Expected decision: N/A
- Stop condition: N/A
- Budget: none

El repositorio local basta ya que se trata de auditar una implementación específica existente de Android intents dentro de la app (Bocatta POS).

## Contexto Local Leido

- Constitution: `openspec/constitution.md`
- AGENTS: Sí (skills)
- Skill registry: `android-intents-sharing`
- Archivos inspeccionados:
  - `GenerarTicketWhatsAppUseCase.kt`
  - `TicketUtils.kt`
  - `DialogosVentas.kt`
  - `AdminScreen.kt`
  - `TabSueldos.kt`
  - `CierreCajaScreen.kt`
  - `ReportScreen.kt`

## Hallazgos del Audit (Intents & Sharing)

Actualmente existen **dos patrones distintos** en la aplicación para compartir texto (tickets, reportes, diagnósticos):

1. **Patrón "WhatsApp First" (Recomendado según la skill):**
   - Presente en: `DialogosVentas.kt` (Tickets), `TabSueldos.kt` (Nómina), `AdminScreen.kt` (Diagnóstico).
   - Implementación: Crea un `Intent.ACTION_SEND` de tipo `text/plain`, establece `setPackage("com.whatsapp")` y lo lanza. Atrapa `ActivityNotFoundException` (u otra `Exception`) si no está instalado WhatsApp, en cuyo caso usa un `Intent.createChooser()` con un intent genérico de fallback sin `setPackage`.
   - Incluye logs de tracking: `LogHelper.recordBreadcrumb` (ej. "share_ticket_whatsapp", "share_ticket_fallback").

2. **Patrón "Generic Chooser Only":**
   - Presente en: `CierreCajaScreen.kt` (Cierre de Caja) y `ReportScreen.kt` (Reportes Diarios).
   - Implementación: Crea un `Intent.ACTION_SEND` genérico y lanza directamente el `Intent.createChooser()`, requiriendo que el usuario escoja la app cada vez, lo que disminuye la fricción si el usuario no tiene WhatsApp, pero agrega un paso extra si siempre usa WhatsApp.
   - Estos lugares no registran si se usa WhatsApp explícitamente y tampoco siguen la recomendación estricta de intentar WhatsApp primero si así lo pide el flujo.

## Análisis de Contenido Compartido

- **Tickets (`GenerarTicketWhatsAppUseCase.kt`)**: Están formateados como texto plano compatible y amigable para WhatsApp con marcadores Markdown (*negrita*, _cursiva_). No incluyen información sensible, solo folio, sucursal, total, desglose y propina.
- **Nómina (`TabSueldos.kt`)**: El reporte (`generarTextoNominaWhatsApp`) respeta los límites de la nómina administrativa y usa el fallback adecuadamente.
- **Fallback**: El Intent fallback `ACTION_SEND` está bien formado (`type = "text/plain"`) y no se usan FileProviders donde el texto plano es suficiente.

## Alternativas

### Opcion A: Mantener el estado actual (Decentralizado)

- Descripcion: Dejar las implementaciones como están, permitiendo que algunas pantallas usen "WhatsApp First" y otras usen "Generic Chooser".
- Ventajas: Ningún esfuerzo de desarrollo.
- Costos: Inconsistencia en la UI/UX y duplicación de código en los manejos de try/catch de Intents. Riesgo de errores si el texto no es plano.
- Riesgos: Si hay una nueva pantalla que requiera sharing, el desarrollador podría volver a duplicar el código del intent.

### Opcion B: Unificar en un helper o use case (Recomendado)

- Descripcion: Crear un archivo `IntentUtils.kt` (o agregar al `TicketUtils.kt`) una función `shareTextWhatsAppWithFallback(context, texto, subject)` y reemplazar en los 5 archivos.
- Ventajas: Consistencia en la app, tracking centralizado con `LogHelper`, respeta las reglas de la skill en toda la app.
- Costos: Requiere refactor menor.
- Riesgos: Impacto mínimo, pero la UX de cierre de caja cambiará a intentar WhatsApp directamente.

## Decision Recomendada

- Opcion B: Centralizar la lógica de intents y fallback.

## Preguntas Que Deben Aclararse Antes de Proponer

- ¿Debe `CierreCajaScreen.kt` y `ReportScreen.kt` también dar prioridad a WhatsApp (WhatsApp First) o el usuario del negocio prefiere escoger otras apps (email, impresión) para esos reportes administrativos?
