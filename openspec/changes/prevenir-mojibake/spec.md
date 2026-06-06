# Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`
Capability: `proceso`
Fecha: `2026-06-04`

## Problema

El workspace puede acumular texto mal decodificado en documentos o codigo sin que Gradle falle. Esto afecta al equipo y puede terminar en UI, tickets, reportes o documentacion ilegible.

## Objetivos

- Detectar mojibake en archivos activos antes de que entre a CI o a una revision.
- Mantener el check compatible con Windows y GitHub Actions.
- Evitar que historicos/generados rompan el check normal.

## No Objetivos

- Corregir todo el historico de `dead_code_quarantine`.
- Regenerar documentos externos en `testsprite_tests`.
- Cambiar logica Android.

## Usuarios y Flujos

### Flujo principal

1.
2. El agente ejecuta `.\tools\encoding\Find-Mojibake.ps1`.
3. Si falla, corrige el texto dentro del alcance antes de cerrar.

### Escenarios alternos

- Sin internet: no aplica.
- Reintento/sync: no aplica.
- Error visible: el script lista archivo y linea.
- Permisos/rol: no aplica.

## Requisitos Funcionales

- R1: El check debe detectar caracteres de reemplazo y patrones comunes de texto UTF-8 mal interpretado.
- R2: El check normal debe excluir artefactos generados o historicos conocidos.
- R3: CI debe ejecutar el check antes de Gradle.

## Requisitos No Funcionales

- Rendimiento de mostrador: no aplica.
- Persistencia/offline: no aplica.
- Auditabilidad: el script debe reportar archivo y linea.
- Seguridad/permisos: no aplica.
- Accesibilidad: no aplica.

## Criterios de Aceptacion

- [x] `.\tools\encoding\Find-Mojibake.ps1` pasa en archivos activos.
- [x] `-IncludeGenerated` permite auditoria amplia y reporta historico roto.
- [x] CI contiene un paso `Check mojibake`.

## Riesgos POS

- Ventas: bajo; previene labels corruptos.
- Inventario: bajo.
- Offline/sync: bajo.
- Caja/pagos: bajo.
- Reportes/tickets: medio; previene texto corrupto en salidas compartibles.

## Preguntas Abiertas

- Si se quiere limpiar cuarentena/testsprite, crear una spec separada para evitar diff masivo.
