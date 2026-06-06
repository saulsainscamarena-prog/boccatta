# Review: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`

## Auditoria contra SDD

- Proposal cumplida: si
- Spec cumplida: si
- Design cumplido: si
- Tasks completadas: si

## Hallazgos

| Severidad | Archivo | Linea | Hallazgo | Accion |
|-----------|---------|-------|----------|--------|
| Info | dead_code_quarantine | varias | El modo `-IncludeGenerated` detecta historico con caracteres de reemplazo. | Mantener fuera del check normal o limpiar en otra spec. |

## Guardrails

- [x] Sin cambios fuera de alcance.
- [x] Sin reversion de cambios del usuario.
- [x] Sin comandos destructivos.
- [x] Sin logica de negocio en Composables.
- [x] Sin dependencias duplicadas.
- [x] Tests/evidencia cubren riesgos criticos.
- [x] Research web usado solo cuando aplicaba y registrado con fuentes primarias.
- [x] No se introdujo una decision externa sin version/fecha/fuente.

## Resultado

- Aprobado: si
- Requiere cambios: no
- Riesgo residual: historicos/generados pueden seguir teniendo texto corrupto hasta una limpieza dedicada.
