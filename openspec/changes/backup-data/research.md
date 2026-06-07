# Research: Audit Android backup data extraction rules

Spec ID: `backup-data`
Capability: `Data Security and Local Storage`
Fecha: `2026-06-06`

## Brief Humano

Audit Android backup data extraction rules and local data handling in Bocatta POS. Review backup_rules.xml, data_extraction_rules.xml, sensitive POS data, local logs, and JSON serialization. Mandoatory: Follow SDD workflow, create research.md, proposal.md, plan.md, do not modify code.

## Web Research Gate

- Research question: How to properly configure data extraction rules and backup rules for Android 12+ regarding local offline databases and sensitive logs?
- Source category from `openspec/source-catalog.yaml`: Documentation
- Why local context is insufficient: N/A
- Expected decision: Update XML files with proper `logs` exclusion and link them in AndroidManifest.xml.
- Stop condition: When the local architecture is fully understood.
- Budget: none

Local context from `android-backup-data-extraction` and `android-data-files` skills is sufficient to make a recommendation.

## Contexto Local Leido

- Constitution: Read and understood.
- AGENTS: Read and understood.
- Skill registry: `android-backup-data-extraction`, `android-data-files` read.
- Source catalog: N/A
- Archivos inspeccionados: `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml`, `OfflineDatabase.kt`, `LogHelper.kt`, `LogCleanupWorker.kt`.

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|
| N/A | N/A | N/A | N/A | N/A |

## Alternativas

### Opcion A

- Descripcion: Dejar `android:allowBackup="false"` y no referenciar los archivos XML.
- Ventajas: Ninguna accion en codigo.
- Costos: Riesgo en Android 12+ donde `device-transfer` todavia podria migrar archivos no excluidos explicitamente a traves de `dataExtractionRules`.
- Riesgos: Fuga de logs operativos y json caches.

### Opcion B

- Descripcion: Declarar `android:fullBackupContent` y `android:dataExtractionRules` en el `<application>` tag de `AndroidManifest.xml`, y agregar los logs (`domain="file" path="logs"`) a las exclusiones en los XML.
- Ventajas: Cumple explicitamente con la seguridad para POS Data en API >= 31 y previene migraciones inseguras.
- Costos: Cambio menor en 3 archivos XML.
- Riesgos: Ninguno significativo; aumenta la privacidad.

## Decision Recomendada

- Opcion B: Linkear las reglas de backup y extraccion en el Manifest, y agregar las carpetas de logs a dichas reglas.

## Suposiciones Externas

- `LogHelper.kt` escribe logs en `filesDir/logs` y JSON serialization ocurre ahi y en Room (que ya excluye `.db`).

## Tradeoffs

- Se confia en la documentacion de Android sobre que `domain="file" path="logs"` excluira la carpeta creada en `context.filesDir`.

## Preguntas Que Deben Aclararse Antes de Proponer

- Ninguna.
