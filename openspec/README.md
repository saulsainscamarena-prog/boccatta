# Bocatta SDD Workflow

Este directorio implementa Spec-Driven Development para Bocatta POS. El objetivo es que cada cambio relevante deje claro el problema, el diseno, las tareas y la verificacion antes de escribir codigo.

## Cuando usarlo

Usa SDD para cambios que toquen ventas, inventario, offline/sync, caja, auth, nomina, reportes, DI, repositorios, modelos de dominio, Firestore, Room/SQLite, WorkManager o UI de mostrador.

Para correcciones triviales de texto, estilos aislados o cambios de una sola linea sin impacto funcional, basta con describir el alcance en el turno de trabajo.

## Flujo

0. Revisar la configuracion del arnes:

```text
openspecfig.yaml
openspec/skill-registry.yaml
openspec/constitution.md
openspec/source-catalog.yaml
openspec/web-research-policy.md
```

Tambien puedes ejecutar el preflight local:

```powershell
.\tools\sdd\Invoke-BocattaSddInit.ps1
```

1. Crear una spec:

```powershell
.\tools\sdd\New-BocattaSpec.ps1 -Id "ajustar-cierre-caja" -Title "Ajustar cierre de caja" -Capability "caja"
```

2. Completar los documentos generados en `openspec/changes/<id>/`:

- `research.md`: briefing tecnico, alternativas y tradeoffs.
- `proposal.md`: alcance, fuera de alcance, riesgos, rollback y criterios de exito.
- `spec.md`: comportamiento esperado, requisitos y escenarios Given/When/Then.
- `design.md`: arquitectura tecnica, contratos, APIs, datos y edge cases.
- `plan.md`: punto de entrada real, archivos afectados, riesgos y estrategia tecnica.
- `tasks.md`: tareas pequenas y ordenadas, con verificacion asociada.
- `review.md`: auditoria contra proposal/spec/design/tasks.
- `verification.md`: comandos Windows, pruebas manuales y evidencia.
- `archive.md`: resumen final para preservar historico.

3. Hacer checkpoints humanos despues de proposal, despues de design y antes de aplicar cambios.

4. Implementar solo las tareas aprobadas. Si `StrictTdd` esta activo, escribir o ajustar pruebas primero, confirmar el fallo esperado cuando sea viable, implementar, pasar a verde y refactorizar.

5. Verificar con el Gradle wrapper de Windows:

```powershell
.\gradlew.bat compileDebugKotlin
```

6. Cuando el cambio quede estable, archivar o promover la spec a `openspec/specs/<capability>/`.

## Principios locales

Lee `openspec/constitution.md` antes de crear o ejecutar una spec. Esa constitution contiene las reglas no negociables de Bocatta POS: offline-first, seguridad de inventario, arquitectura existente, Compose declarativo, Koin, Firebase/Room y Gradle en Windows.

## Research Web

El flujo es local-first. Antes de buscar en internet, llena el `Web Research Gate` de `research.md`: pregunta exacta, categoria de fuente, por que el repo no basta y condicion de parada.

Usa `openspec/source-catalog.yaml` para priorizar fuentes oficiales. Registra URL, fecha, version/alcance y decision soportada. Si dos fuentes primarias actuales responden la pregunta, deja de buscar y vuelve al codigo.

## Convencion de IDs

Usa IDs en kebab-case, pequenos y accionables:

- `bloquear-doble-cobro`
- `mejorar-ticket-whatsapp`
- `sincronizar-mermas-offline`

Evita IDs genericos como `fix`, `cambios`, `v2` o `mejoras`.
