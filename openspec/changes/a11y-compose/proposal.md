# Proposal: a11y-compose

Spec ID: `a11y-compose`
Capability: `Accessibility Compose`
Strict TDD: `no`

## Problema

- `BocattaComponents.kt` carece de propiedades semánticas optimizadas para lectores de pantalla. Elementos lógicos como métricas, filas de resumen y tarjetas de carrito se leen por fragmentos en lugar de entidades agrupadas.
- Algunos títulos de sección no están marcados como encabezados (`heading()`), lo cual dificulta la navegación rápida.
- Faltan descripciones de estado durante los procesos de carga, como cuando un botón se deshabilita temporalmente y muestra un spinner.

## Alcance

- Modificar `BocattaComponents.kt` para agregar `Modifier.semantics`.
- Mejorar `BocattaMetricCard`, `BocattaFilaResumen`, `BocattaCartItemRow`, `BocattaSectionTitle` y `BocattaButton`.

## Fuera de Alcance

- Modificar lógica de negocio o comportamiento de la UI.
- Modificar pantallas enteras más allá de la biblioteca de componentes.
- Alterar flujos offline o de base de datos.

## Archivos Afectados Esperados

- `app/src/main/java/com/bocatta/pos/presentation/ui/components/BocattaComponents.kt`

## Riesgos

- Ventas: N/A.
- Inventario: N/A.
- Offline/sync: N/A.
- Caja/pagos: N/A.
- UI: Que la agrupación de nodos de accesibilidad haga invisible alguna acción secundaria para TalkBack si no se prueba bien, sin embargo, `mergeDescendants` en `Surface`/`Row` respeta las acciones subyacentes.
- Seguridad: N/A.

## Research Externo

- Web requerida: no
- Pregunta investigada: N/A
- Fuentes primarias: N/A
- Decision tomada: Utilizar la API de Semantics de Compose.
- Suposicion sensible a version: N/A

## Estrategia de Rollback

- Revertir los cambios en `BocattaComponents.kt` a través de git en caso de regresiones de UI.

## Criterios de Exito

- [ ] `BocattaMetricCard` consolida su texto para lectura unificada.
- [ ] `BocattaFilaResumen` se lee como un solo elemento.
- [ ] `BocattaSectionTitle` se anuncia como encabezado.
- [ ] `BocattaButton` expone un `stateDescription` cuando está en estado de carga.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
