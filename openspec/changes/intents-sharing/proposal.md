# Proposal: Unificar Android Intents de Compartir

Spec ID: `intents-sharing`
Capability: `Android Intent Action Send`
Strict TDD: `no`

## Problema

- Existen actualmente dos estrategias divergentes para compartir texto en Bocatta POS:
  1. Uso del `setPackage("com.whatsapp")` explícito con manejo de excepciones y fallback al `Intent.createChooser` (Tickets, Nómina, Diagnóstico).
  2. Uso de `Intent.createChooser` sin intentar WhatsApp primero (Cierre de Caja, Reportes).
- Esta duplicación de lógica causa deuda técnica y riesgo de regresiones si el tracking de `LogHelper` falla o se olvida en nuevas implementaciones de compartición.
- Faltan unificaciones en cómo se envía el payload (`Intent.EXTRA_TEXT`).

## Alcance

- Crear una utilidad unificada, por ejemplo `IntentHelper.shareTextWithWhatsAppFallback(context: Context, texto: String, subject: String?, logTag: String)`.
- Refactorizar las 5 clases que disparan Intents de sharing para usar esta nueva utilidad:
  - `DialogosVentas.kt`
  - `AdminScreen.kt`
  - `TabSueldos.kt`
  - `CierreCajaScreen.kt`
  - `ReportScreen.kt`

## Fuera de Alcance

- Modificar el formato de los tickets, la nómina o los reportes en sí. Sólo se aborda el envoltorio de la entrega del SO (Intent).
- Compartir archivos binarios/imágenes (esto sigue las restricciones de usar solo text/plain).

## Archivos Afectados Esperados

- `core/IntentHelper.kt` (o similar, nuevo o modificando `TicketUtils`)
- `presentation/ui/components/DialogosVentas.kt`
- `presentation/ui/screens/admin/AdminScreen.kt`
- `presentation/ui/screens/admin/TabSueldos.kt`
- `presentation/ui/screens/caja/CierreCajaScreen.kt`
- `presentation/ui/screens/reportes/ReportScreen.kt`

## Riesgos

- Ventas: Bajo. La generación del ticket sigue igual, sólo cambia el botón de compartir final.
- Inventario: N/A.
- Offline/sync: N/A.
- Caja/pagos: Bajo. El cierre de caja ahora intentaría ir a WhatsApp primero; esto necesita la confirmación del product owner si el comportamiento anterior (chooser directo) era intencional o un descuido.
- UI: Bajo, mejora la consistencia.
- Seguridad: El texto viaja fuera del sandbox de la app (al OS) por intents explícitos (`ACTION_SEND`). Manteniendo text/plain y revisando no exportar llaves/IDs es seguro.

## Research Externo

- Web requerida: no
- Decision tomada: Reutilizar APIs estables de Android SDK de Intents explícitos. Fallbacks a generic Chooser.

## Estrategia de Rollback

- Revertir los commits del refactor a las pantallas; al no tener estado persistente en la DB o Room, el rollback es inmediato.

## Criterios de Exito

- [ ] Hay un solo punto en la codebase que defina el Intent de compartición (`Intent.ACTION_SEND` + `setPackage("com.whatsapp")`).
- [ ] Todas las 5 características (Tickets, Nómina, Diagnóstico, Caja, Reportes) usan la misma función de compartición.
- [ ] Si el usuario no tiene WhatsApp instalado, se lanza un "chooser" genérico sin que la app crashee.
- [ ] Las métricas y logs de `LogHelper` continúan disparándose.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada. Especialmente sobre la intención de cambiar el Chooser explícito de Cierre de Caja al "WhatsApp First" estándar.
