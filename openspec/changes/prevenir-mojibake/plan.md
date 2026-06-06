# Plan Tecnico: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Archivos propietarios del flujo real antes de editar codigo.

## Punto de Entrada Real

Describe donde vive hoy el comportamiento y por que ese es el owner correcto.

- UI: no aplica.
- ViewModel: no aplica.
- Dominio/use case: no aplica.
- Repositorio/data: no aplica.
- DI: no aplica.
- Tests: `tools/encoding/Find-Mojibake.ps1` y CI.

## Archivos Probables

- `.github/workflows/ci.yml`
- `tools/encoding/Find-Mojibake.ps1`
- `tools/encoding/README.md`
- `tools/sdd/Invoke-BocattaSddInit.ps1`
- `openspec/changes/prevenir-mojibake/*`

## Estrategia

1. Validar si ya hay configuracion UTF-8 local.
2. Ejecutar detector existente y ajustar falsos positivos.
3. Conectar detector a CI y documentarlo.

## Contratos a Revisar

- Modelos: no aplica.
- Interfaces: no aplica.
- Koin: no aplica.
- Firestore/Room: no aplica.
- WorkManager/sync: no aplica.

## Impacto Offline

- Venta online: no aplica.
- Venta offline: no aplica.
- Reconexion: no aplica.
- Deduccion de inventario: no aplica.
- Idempotencia/reintentos: no aplica.

## Impacto UI

- Movil: no aplica.
- Tablet: no aplica.
- Estados de carga/error: no aplica.
- Eventos consumibles: no aplica.
- Accesibilidad: no aplica.

## Plan de Pruebas

- Unit: no aplica.
- Instrumented: no aplica.
- Compose UI: no aplica.
- Maestro/manual: no aplica.
- Gradle: no aplica porque no cambia Kotlin/Compose.
- Tooling: `.\tools\encoding\Find-Mojibake.ps1`, `.\tools\sdd\Invoke-BocattaSddInit.ps1`.

## Criterio para No Continuar

Enumera condiciones que deben detener implementacion hasta aclarar alcance o datos.

- Si el check normal falla por historico/generados, revisar exclusiones antes de ampliar alcance.
