# Proposal: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`
Capability: `proceso`
Strict TDD: `false`

## Problema

- El repo puede recibir texto mal decodificado sin que Gradle lo detecte. Eso da documentacion ilegible y puede llegar a labels/tickets si entra en Kotlin.

## Alcance

- Reutilizar y endurecer `tools/encoding/Find-Mojibake.ps1`.
- Documentar el uso del detector.
- Ejecutar el detector en CI antes de Gradle.
- Registrar la decision en esta spec.

## Fuera de Alcance

- Limpieza global de `testsprite_tests`, `dead_code_quarantine` o artefactos `audit-avd`.
- Reescritura de textos Kotlin con acentos/emojis legitimos.
- Cambios de logica Android.

## Archivos Afectados Esperados

- `.github/workflows/ci.yml`
- `tools/encoding/Find-Mojibake.ps1`
- `tools/encoding/README.md`
- `tools/sdd/Invoke-BocattaSddInit.ps1`
- `openspec/changes/prevenir-mojibake/*`

## Riesgos

- Ventas:
- Inventario:
- Offline/sync:
- Caja/pagos:
- UI:
- Seguridad: bajo; no cambia app ni datos.

## Research Externo

- Web requerida: no
- Pregunta investigada: no aplica
- Fuentes primarias: repo local
- Decision tomada: usar check local/CI
- Suposicion sensible a version: GitHub Ubuntu runner mantiene `pwsh` disponible.

## Estrategia de Rollback

- Revertir el paso `Check mojibake` de CI y/o ejecutar el script manualmente si causara falsos positivos.

## Criterios de Exito

- [ ] `.\tools\encoding\Find-Mojibake.ps1` pasa en el workspace activo.
- [ ] CI ejecuta el detector antes de Gradle.
- [ ] El detector puede auditar generados con `-IncludeGenerated`.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
