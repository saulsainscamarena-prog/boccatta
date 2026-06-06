# Archive: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`
Capability: `proceso`
Fecha: `2026-06-04`

## Resultado Final

- Se agrego prevencion automatica contra mojibake recurrente en archivos activos.

## Cambios Entregados

- Detector PowerShell endurecido.
- README de encoding.
- Paso `Check mojibake` en CI.
- Preflight SDD ahora anuncia el check de encoding.

## Verificacion Ejecutada

- `.\tools\encoding\Find-Mojibake.ps1`
- `.\tools\sdd\Invoke-BocattaSddInit.ps1`
- `.\tools\encoding\Find-Mojibake.ps1 -IncludeGenerated` para confirmar deuda historica excluida.

## Decisiones Preservadas

- Local-first; no se uso web.
- Excluir historicos/generados del check normal evita fallas por deuda antigua.
- El modo amplio queda disponible para auditorias.

## Pendientes o Deuda Tecnica

- Limpieza futura de `dead_code_quarantine` y `testsprite_tests` si se decide conservarlos como documentos legibles.

## Ubicacion de Archivo

Cuando el cambio este cerrado, mover esta carpeta a:

```text
openspec/changes/archive/2026-06-04-prevenir-mojibake/
```
