# Research - Auditoría de Calidad de Código

Este documento documenta la información inicial recopilada durante la auditoría de calidad de código y buenas prácticas.

## Herramientas de Análisis Estático
- **Detekt:** Configurado. Verificando reglas de `detekt.yml`.
- **Ktlint:** Configurado mediante alias de plugin `libs.plugins.ktlint`.

## Focos de Inspección (AGENTS.md)
1. Lógica de negocio fuera de Composables.
2. ViewModels como única fuente de estado UI (UDF).
3. Reducción de recomposiciones (Jank) con `derivedStateOf`.
4. Manejo robusto offline (`OfflineManager`, `SyncWorker`).

## Resultados Iniciales (Detekt)
- **Code Smells Totales:** 280
- **Principal Infracción:** `WildcardImport` (280 incidencias).
- **Acción a tomar:** Reemplazar todos los `import .*` por importaciones explícitas (ej. en `SalesViewModelV2`, `SessionViewModel`, y múltiples pantallas de Jetpack Compose).
