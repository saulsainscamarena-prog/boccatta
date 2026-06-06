# Design: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`

## Resumen Tecnico

- Usar un script PowerShell local como check deterministico de mojibake.
- Ejecutarlo en CI con `pwsh` antes de tareas Gradle.
- Mantener exclusiones explicitas para historicos/generados que ya contienen texto roto.

## Dependencias de Documentacion Externa

- Android/Compose: no aplica.
- Firebase: no aplica.
- Gradle/JDK: no aplica.
- Kotlin/Koin: no aplica.
- Seguridad/Play Store: no aplica.
- No aplica porque: el problema se resuelve con politica local de encoding y CI.

## Mapeo Requisito a Implementacion

| Requisito | Owner actual | Cambio tecnico | Prueba |
|-----------|--------------|----------------|--------|
| R1 | `tools/encoding/Find-Mojibake.ps1` | Detectar caracteres sospechosos con archivo y linea | `.\tools\encoding\Find-Mojibake.ps1` |
| R2 | `tools/encoding/Find-Mojibake.ps1` | Excluir historicos/generados en modo normal y permitir `-IncludeGenerated` | ejecutar ambos modos |
| R3 | `.github/workflows/ci.yml` | Agregar paso `Check mojibake` con `pwsh` | inspeccion del workflow |

## APIs, Contratos y Datos

- Modelos: no aplica.
- Interfaces: no aplica.
- Repositorios: no aplica.
- Use cases: no aplica.
- DI/Koin: no aplica.
- Firestore: no aplica.
- Room/SQLite: no aplica.
- WorkManager: no aplica.

## Estructuras de Salida

Si aplica JSON, ticket, reporte o payload sync, documentar formato.

```json
{
  "path": "relative/file/path",
  "line": 1,
  "reason": "possible mojibake"
}
```

## Edge Cases

- Sin internet: no aplica.
- Reconexion: no aplica.
- Doble toque/reintento: no aplica.
- Rol no autorizado: no aplica.
- Datos corruptos/incompletos: el check falla con archivo y linea.

## Guardrails Aplicados

- [x] Negocio fuera de Composables.
- [x] Estado en ViewModel.
- [x] Repositorios/use cases existentes reutilizados.
- [x] Offline auditable si aplica.
- [x] Deduccion de inventario validada si aplica.

## Checkpoint Humano

No pasar a `tasks.md` hasta que el diseno este aceptado.
