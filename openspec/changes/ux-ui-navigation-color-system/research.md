# Research: Sistema UX/UI, navegacion y color

Spec ID: `ux-ui-navigation-color-system`
Capability: `Compose UX/UI`
Fecha: `2026-06-18`

## Brief Humano

El usuario pidio corregir integralmente los faltantes detectados en botones, navegacion, color, diseno general, accesibilidad y adaptacion movil/tablet de Bocatta POS.

## Web Research Gate

- Research question: No aplica; se usan APIs Material 3 ya presentes y patrones locales.
- Source category: Android oficial, si fuera necesario.
- Why local context is insufficient: No aplica.
- Expected decision: Mantener dependencias actuales y corregir owners existentes.
- Stop condition: El repositorio y las skills locales cubren las decisiones.
- Budget: none; no se introduce API o dependencia nueva y las skills fueron verificadas en mayo/junio de 2026.

## Contexto Local Leido

- `openspec/constitution.md`, instrucciones AGENTS y `openspec/skill-registry.yaml`.
- Skills: `styles`, `android-m3-ui-design`, `adaptive`, `android-accessibility-compose`.
- Tema/color, componentes comunes, ventas, inventario, admin, navegacion, strings y pruebas Compose.

## Fuentes Externas Consultadas

No se consultaron fuentes externas.

## Alternativas

### Opcion A: Compose Styles y Navigation 3

- Ventajas: APIs adaptativas recientes.
- Costos: Compose alpha, compileSdk 37 y cambios Gradle.
- Riesgos: incompatibilidad con el port Windows y alcance innecesario.

### Opcion B: Material 3 estable actual

- Ventajas: cambio quirurgico sin dependencias y compatible con la arquitectura actual.
- Costos: navegacion adaptativa con componentes M3 existentes.
- Riesgos: Navigation 3 queda como migracion futura.

## Decision Recomendada

Aplicar la opcion B: tokens semanticos, contraste, targets de 48 dp, navegacion persistente en tablet, Admin adaptable y pruebas Compose focalizadas.

## Suposiciones Externas

Ninguna.

## Tradeoffs

- Se prioriza estabilidad sobre APIs experimentales.
- Se extraen textos tocados, sin automatizacion masiva fuera de alcance.

## Preguntas Que Deben Aclararse Antes de Proponer

Ninguna; el usuario autorizo implementar el paquete auditado.
