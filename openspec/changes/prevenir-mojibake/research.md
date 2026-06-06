# Research: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`
Capability: `proceso`
Fecha: `2026-06-04`

## Brief Humano

Resume el pedido original, contexto del negocio y restricciones conocidas.

- El usuario quiere eliminar el problema recurrente de mojibake en el workspace.
- El repo ya contiene `.editorconfig` y `.gitattributes` sin trackear con politica UTF-8/LF razonable.
- Hay mojibake real en historicos de `dead_code_quarantine` y un PRD generado en `testsprite_tests`; no conviene hacer limpieza global dentro de esta prueba.

## Web Research Gate

Completar antes de buscar en internet.

- Research question:
- Source category from `openspec/source-catalog.yaml`:
- Why local context is insufficient:
- Expected decision:
- Stop condition:
- Budget: none

Si `Budget` es `none`, explicar por que el repo local basta.

El problema es local de codificacion y politicas del repo. No se requiere web para decidir: basta inspeccionar archivos, `.editorconfig`, `.gitattributes`, CI y scripts locales.

## Contexto Local Leido

- Constitution:
- AGENTS: regla de corregir mojibake dentro del alcance tocado.
- Skill registry: no aplica una skill Android.
- Source catalog: no aplica por `Budget: none`.
- Archivos inspeccionados: `.editorconfig`, `.gitattributes`, `.github/workflows/ci.yml`, `tools/encoding/Find-Mojibake.ps1`, `testsprite_tests/PRD_Bocatta.md`, `dead_code_quarantine/2026-05-21/*.txt`.

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|
| No aplica |  | 2026-06-04 | Local-only | No usar web |

## Alternativas

### Opcion A

- Descripcion:
- Mantener solo `.editorconfig` y `.gitattributes`.
- Ventajas: simple.
- Costos: no detecta mojibake ya introducido.
- Riesgos: el problema puede seguir entrando por prompts, copias o herramientas externas.

### Opcion B

- Descripcion:
- Agregar verificador PowerShell y ejecutarlo en CI.
- Ventajas: falla rapido, funciona en Windows y Ubuntu CI, evita ruido en Gradle.
- Costos: puede dar falsos positivos con caracteres raros legitimos.
- Riesgos: si se escanean generados/cuarentena, CI fallaria por historico conocido.

## Decision Recomendada

- Opcion B: verificador activo con exclusiones explicitas para generados/historico, mas documentacion.

## Suposiciones Externas

- `.editorconfig` y `.gitattributes` se conservaran como base UTF-8, pero la prevencion real queda en el script y CI.

## Tradeoffs

- Se excluyen `testsprite_tests`, `dead_code_quarantine` y `audit-avd` del check normal para no mezclar historico/generados con codigo activo.

## Preguntas Que Deben Aclararse Antes de Proponer

- Si despues se quiere limpiar historicos, hacerlo en otra spec para evitar un diff grande.
