# Tasks: Sistema UX/UI, navegacion y color

Spec ID: `ux-ui-navigation-color-system`

## Tareas

- [x] 1. Confirmar owners y preservar cambios locales.
- [x] 2. Implementar tokens semanticos y boton comun.
- [x] 3. Corregir inventario y pantallas operativas relacionadas.
- [x] 4. Implementar navegacion movil/tablet.
- [x] 5. Adaptar Admin y targets tactiles.
- [x] 6. Agregar pruebas Compose.
- [ ] 7. Ejecutar pre-build, compilacion y tests.
  - `compileDebugKotlin`: aprobado.
  - Android tests: bloqueados por errores preexistentes en `core/database` y `app/src/androidTest`.
- [ ] 8. Completar review, verification y archive.
  - Review y verification documentados; archive queda pendiente hasta sanear el harness AndroidTest.

## Checklist Critico

- [x] Checkout, offline e inventario funcional no cambian.
- [x] No se agregan dependencias.
- [x] Errores visibles y semantica preservados.
- [x] Sin archivos fuera de alcance del paquete UX/UI.
