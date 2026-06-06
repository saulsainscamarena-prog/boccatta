# Encoding Checks

Herramientas para prevenir mojibake recurrente en Bocatta.

## Verificacion normal

```powershell
.\tools\encoding\Find-Mojibake.ps1
```

Escanea archivos activos de texto y codigo. Excluye artefactos generados o historicos:

- `dead_code_quarantine`
- `testsprite_tests`
- `audit-avd`
- `build`
- `.gradle`

## Verificacion amplia

```powershell
.\tools\encoding\Find-Mojibake.ps1 -IncludeGenerated
```

Incluye artefactos generados y cuarentena para auditorias puntuales. No se usa por defecto porque esos directorios ya contienen historico roto o salidas de herramientas externas.

## Politica

- Archivos de texto nuevos deben guardarse como UTF-8.
- `.editorconfig` define `charset = utf-8`.
- `.gitattributes` normaliza finales de linea sin convertir binarios.
- Si aparecen secuencias tipicas de mojibake o caracteres de reemplazo en texto activo, corregirlos dentro del alcance antes de cerrar la tarea.
