# Sistema UX/UI, navegacion y color

Spec ID: `ux-ui-navigation-color-system`
Capability: `Compose UX/UI`
Fecha: `2026-06-18`

## Problema

La UI es inconsistente segun tema y ventana, con accesos operativos ocultos en tablet y algunos targets tactiles pequenos.

## Objetivos

- Preservar velocidad de mostrador en movil/tablet.
- Mantener contraste y significado de color en todos los temas.
- Hacer coherentes botones, navegacion y layouts tocados.
- Mejorar accesibilidad sin ruido visual.

## No Objetivos

- Cambiar ventas, inventario funcional, caja u offline.
- Adoptar APIs experimentales.

## Usuarios y Flujos

### Flujo principal

1. El vendedor abre ventas.
2. En movil abre el menu; en tablet ve accesos persistentes.
3. Online/offline conserva significado visual estable.
4. Navega a caja, inventario o admin con targets adecuados.

### Escenarios alternos

- Sin internet: offline usa rol error y texto explicito.
- Reintento/sync: sin cambios funcionales.
- Error visible: banners/snackbars existentes permanecen.
- Permisos/rol: Admin sigue condicionado por `esAdmin`.

## Requisitos Funcionales

- R1: Exponer tokens success/warning con contenido contrastante.
- R2: InventoryScreen usa roles, strings y tipografia M3.
- R3: Ventas muestra rail persistente desde 720 dp y drawer debajo.
- R4: Caja/Cerrar turno no son destinos duplicados equivalentes.
- R5: Admin adapta columnas y permite scroll natural.
- R6: Controles explicitos menores a 48 dp se corrigen.
- R7: Interactivos ofrecen etiqueta o semantica suficiente.

## Requisitos No Funcionales

- Rendimiento: sin calculo pesado o recomposicion global nueva.
- Persistencia/offline: sin cambios.
- Auditabilidad: cambios por owner y pruebas focalizadas.
- Accesibilidad: 48 dp, roles y descripciones claras.

## Criterios de Aceptacion

- [ ] Given tema claro/dinamico, when abre inventario, then texto/bordes contrastan.
- [ ] Given tablet >=720 dp, when abre ventas, then navegacion queda visible.
- [ ] Given movil <720 dp, when abre ventas, then drawer sigue disponible.
- [ ] Given color dinamico, when online, then usa success estable.
- [ ] Given Admin en movil/tablet, then tarjetas fluyen sin altura rigida.
- [ ] Given controles +/- o editar/eliminar, then target >=48 dp.

## Riesgos POS

- Ventas: no tocar carrito/checkout.
- Inventario: solo presentacion.
- Offline/sync, caja/pagos, reportes: sin cambios funcionales.

## Preguntas Abiertas

Ninguna.
