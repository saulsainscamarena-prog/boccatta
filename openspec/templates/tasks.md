# Tasks: {{TITLE}}

Spec ID: `{{ID}}`

## Reglas

- Cada tarea debe ser pequena, auditable y reversible.
- No mezclar refactor amplio con correccion funcional.
- Marcar verificacion real por tarea o explicar por que no aplica.

## Tareas

- [ ] 1. Confirmar owner actual del comportamiento.
  - Archivos:
  - Verificacion:

- [ ] 2. Ajustar contratos/modelos si aplica.
  - Archivos:
  - Verificacion:

- [ ] 3. Implementar logica fuera de Composables.
  - Archivos:
  - Verificacion:

- [ ] 4. Conectar estado/eventos en ViewModel.
  - Archivos:
  - Verificacion:

- [ ] 5. Actualizar UI Compose.
  - Archivos:
  - Verificacion:

- [ ] 6. Cubrir pruebas criticas.
  - Archivos:
  - Verificacion:

- [ ] 7. Ejecutar verificacion final Windows.
  - Comando: `{{GRADLE_VERIFY}}`
  - Resultado:

## Checklist Critico

- [ ] Checkout protegido contra doble toque si toca ventas/pagos.
- [ ] Venta offline conserva folio, monto, items y estado sincronizable si aplica.
- [ ] Deduccion de inventario validada online/offline si aplica.
- [ ] Errores visibles, sin fallos silenciosos.
- [ ] No se agregaron dependencias duplicadas.
- [ ] No se tocaron archivos fuera del alcance.
