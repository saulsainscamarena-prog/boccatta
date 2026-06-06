# Verification: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`

## Entorno

- Windows: PowerShell local
- JDK: no aplica
- Gradle wrapper: no aplica
- Emulador/dispositivo: no aplica
- Fecha: 2026-06-04

## Comandos

```powershell
.\tools\encoding\Find-Mojibake.ps1
.\tools\sdd\Invoke-BocattaSddInit.ps1
.\tools\encoding\Find-Mojibake.ps1 -IncludeGenerated
```

## Resultados

- Compilacion: no aplica, no hubo Kotlin/Compose.
- Unit tests: no aplica.
- Instrumented tests: no aplica.
- Manual: inspeccion de `.github/workflows/ci.yml`.
- `Find-Mojibake.ps1`: pasa en archivos activos.
- `Invoke-BocattaSddInit.ps1`: pasa.
- `Find-Mojibake.ps1 -IncludeGenerated`: falla esperado por historico en `dead_code_quarantine`.

## Pruebas Manuales

### Movil

- [x] No aplica.

### Tablet

- [x] No aplica.

### Offline

- [x] Sin internet: no aplica.
- [x] Reconexion: no aplica.
- [x] Sync: no aplica.

## Evidencia

- Logs: salida de comandos en esta sesion.
- Capturas: no aplica.
- Observaciones: el modo amplio confirma deuda historica fuera del check normal.

## Incidencias

- No se limpio `dead_code_quarantine` para evitar diff grande y cambios fuera del objetivo preventivo.
