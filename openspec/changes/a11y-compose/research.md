# Research: a11y-compose

Spec ID: `a11y-compose`
Capability: `Accessibility Compose`
Fecha: `2026-06-06`

## Brief Humano

Auditar y agregar semántica de accesibilidad (contentDescription, touch targets, focus) al archivo `BocattaComponents.kt`. El objetivo es cumplir con las directrices WCAG mejorando la usabilidad para lectores de pantalla y navegación por teclado en un entorno de punto de venta (POS).

## Web Research Gate

- Research question: N/A
- Source category from `openspec/source-catalog.yaml`: N/A
- Why local context is insufficient: Contexto local es suficiente.
- Expected decision: N/A
- Stop condition: N/A
- Budget: none

Contexto local basta porque `android-accessibility-compose` SKILL y el conocimiento de Material 3 / WCAG son suficientes para aplicar semánticas estándar de Compose a los componentes existentes.

## Contexto Local Leido

- Constitution: `openspec/constitution.md`
- AGENTS: `AGENTS.md` y `android-accessibility-compose` SKILL
- Skill registry: N/A
- Source catalog: N/A
- Archivos inspeccionados: `BocattaComponents.kt`

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|
| Ninguna | N/A | N/A | N/A | N/A |

## Alternativas

### Opcion A

- Descripcion: Utilizar `Modifier.semantics(mergeDescendants = true)` en contenedores de lectura lógica como tarjetas y filas de resumen para evitar pausas innecesarias en lectores de pantalla.
- Ventajas: Mejora drásticamente la experiencia del lector de pantalla agrupando información relacionada.
- Costos: Mínimo impacto en el rendimiento.
- Riesgos: Ninguno.

## Decision Recomendada

- Aplicar agrupaciones de semántica (`mergeDescendants = true`) en `BocattaMetricCard`, `BocattaFilaResumen` y `BocattaCartItemRow`.
- Marcar títulos con `Modifier.semantics { heading() }` en `BocattaSectionTitle`.
- Añadir descripciones de estado a componentes interactivos cuando cargan (e.g. `BocattaButton`).

## Suposiciones Externas

- Material 3 Compose maneja automáticamente el tamaño del touch target mínimo de 48dp para sus componentes interactivos (como `IconButton` o `Button`).

## Tradeoffs

- Agrupar semántica puede hacer que todo el texto de una fila se lea junto. Es necesario asegurar que suene coherente.

## Preguntas Que Deben Aclararse Antes de Proponer

- Ninguna.
