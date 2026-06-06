# Tasks: Prevenir mojibake recurrente

Spec ID: `prevenir-mojibake`

## Reglas

- Cada tarea debe ser pequena, auditable y reversible.
- No mezclar refactor amplio con correccion funcional.
- Marcar verificacion real por tarea o explicar por que no aplica.

## Tareas

- [x] 1. Confirmar owner actual del comportamiento.
  - Archivos: `.editorconfig`, `.gitattributes`, `.github/workflows/ci.yml`, `tools/encoding/Find-Mojibake.ps1`
  - Verificacion: lectura de archivos y busqueda dirigida de patrones mojibake.

- [x] 2. Endurecer detector de mojibake.
  - Archivos: `tools/encoding/Find-Mojibake.ps1`
  - Verificacion: `.\tools\encoding\Find-Mojibake.ps1`

- [x] 3. Documentar politica de encoding.
  - Archivos: `tools/encoding/README.md`
  - Verificacion: lectura del README y check ASCII en archivos tocados.

- [x] 4. Conectar detector al arnes SDD.
  - Archivos: `tools/sdd/Invoke-BocattaSddInit.ps1`
  - Verificacion: `.\tools\sdd\Invoke-BocattaSddInit.ps1`

- [x] 5. Ejecutar detector en CI.
  - Archivos: `.github/workflows/ci.yml`
  - Verificacion: inspeccion del workflow.

- [x] 6. Ejecutar verificacion final Windows.
  - Comando: `.\tools\encoding\Find-Mojibake.ps1`
  - Resultado: pendiente registrar en `verification.md`.

## Checklist Critico

- [ ] Checkout protegido contra doble toque si toca ventas/pagos.
- [ ] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [ ] Deduccion de inventario validada online/offline si aplica.
- [ ] Errores visibles, sin fallos silenciosos.
- [x] No se agregaron dependencias duplicadas.
- [x] No se tocaron archivos fuera del alcance.
