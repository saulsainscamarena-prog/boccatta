# Proposal: Sistema UX/UI, navegacion y color

Spec ID: `ux-ui-navigation-color-system`
Capability: `Compose UX/UI`
Strict TDD: `no`

## Problema

La app combina Material 3 con colores fijos, estados atados a acentos variables, controles menores a 48 dp, navegacion modal en tablet y layouts administrativos rigidos.

## Justificacion del Soft Gate

Hay mas de tres cambios SDD abiertos. Los cambios previos de M3, accesibilidad e i18n estan incompletos, referencian rutas antiguas o automatizaciones masivas. Este cambio integrador se limita a owners actuales y no modifica Gradle, dominio, checkout, inventario funcional u offline.

## Alcance

- Tokens success/warning y contraste claro/oscuro/dinamico.
- Botones comunes y targets tactiles de 48 dp.
- Navegacion persistente de ventas en tablet y drawer en movil.
- Eliminar ambiguedad Caja/Cerrar turno.
- Dashboard Admin adaptable sin altura fija.
- Textos tocados, semantica y pruebas Compose.

## Fuera de Alcance

- Navigation 3, Compose Styles experimental, dependencias o logica de negocio.
- Extraccion global automatizada de strings.

## Archivos Afectados Esperados

- Tema, strings y componentes de `core/ui`.
- Ventas: `SalesScreen.kt`, `SalesScreenSections.kt`, `AperturaDiaScreen.kt`.
- Inventario: `InventoryScreen.kt`.
- Admin: `AdminScreen.kt`, `TabPromociones.kt`.
- Pruebas Compose focalizadas.

## Riesgos

- Ventas: preservar carrito/checkout al cambiar solo contenedores visuales.
- Inventario: no tocar deduccion o persistencia.
- Offline/sync: sin cambios.
- Caja/pagos: solo etiqueta de navegacion.
- UI: regresion de tamanos o contraste.
- Seguridad: sin cambios.

## Research Externo

- Web requerida: no.
- Decision: Material 3 estable sin dependencias.
- Suposicion sensible a version: ninguna.

## Estrategia de Rollback

Revertir solo los archivos listados; no existen migraciones de datos.

## Criterios de Exito

- [ ] InventoryScreen no fuerza blanco sobre superficies de tema.
- [ ] Online/warning no dependen de `tertiary` dinamico.
- [ ] Targets explicitos son de al menos 48 dp.
- [ ] Tablet tiene navegacion persistente; movil conserva drawer.
- [ ] Admin usa columnas adaptables sin altura fija.
- [ ] Compilacion y tests focalizados pasan.

## Checkpoint Humano

Aprobado por la instruccion explicita: "ok hazlo todo".
