# SDD Windows Tools

Herramientas PowerShell para crear specs de Bocatta POS en Windows.

## Crear una spec

```powershell
.\tools\sdd\New-BocattaSpec.ps1 -Id "bloquear-doble-cobro" -Title "Bloquear doble cobro" -Capability "ventas"
```

## Preflight SDD Init

```powershell
.\tools\sdd\Invoke-BocattaSddInit.ps1
```

Esto crea:

```text
openspec/changes/bloquear-doble-cobro/research.md
openspec/changes/bloquear-doble-cobro/proposal.md
openspec/changes/bloquear-doble-cobro/spec.md
openspec/changes/bloquear-doble-cobro/design.md
openspec/changes/bloquear-doble-cobro/plan.md
openspec/changes/bloquear-doble-cobro/tasks.md
openspec/changes/bloquear-doble-cobro/review.md
openspec/changes/bloquear-doble-cobro/verification.md
openspec/changes/bloquear-doble-cobro/archive.md
```

## Strict TDD

El valor por defecto es estricto. Para dejarlo explicito:

```powershell
.\tools\sdd\New-BocattaSpec.ps1 -Id "bloquear-doble-cobro" -Title "Bloquear doble cobro" -Capability "ventas" -StrictTdd
```

Para desactivarlo en cambios sin harness razonable:

```powershell
.\tools\sdd\New-BocattaSpec.ps1 -Id "actualizar-docs-sdd" -Title "Actualizar docs SDD" -Capability "proceso" -NoStrictTdd
```

## Sobrescribir una spec en borrador

```powershell
.\tools\sdd\New-BocattaSpec.ps1 -Id "bloquear-doble-cobro" -Title "Bloquear doble cobro" -Capability "ventas" -Force
```

Usa `-Force` solo para specs que aun no tengan trabajo real.
