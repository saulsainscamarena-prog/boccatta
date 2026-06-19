# Plan Tecnico: Sistema UX/UI, navegacion y color

Spec ID: `ux-ui-navigation-color-system`

## Lectura Inicial Obligatoria

- AGENTS, constitution, skill registry y owners UI actuales.

## Punto de Entrada Real

- UI: `core/ui` y pantallas de ventas, inventario y admin.
- ViewModel/dominio/repositorio/DI: sin cambios.
- Tests: `app/src/androidTest`.

## Estrategia

1. Tokens y boton comun.
2. Inventario y targets menores.
3. Navegacion adaptable en ventas.
4. Admin adaptable.
5. Pruebas y verificacion.

## Impacto Offline

- Venta online/offline, reconexion, deduccion e idempotencia: sin cambios.

## Impacto UI

- Movil: drawer y contenido compacto.
- Tablet: navegacion persistente.
- Carga/error: se conservan estados.
- Accesibilidad: targets, semantica y contraste.

## Plan de Pruebas

- Compose UI: navegacion y semantica.
- Manual: movil/tablet si hay emulador.
- Gradle: `compileDebugKotlin` y tests focalizados.

## Criterio para No Continuar

- Solapamiento no resoluble con checkout o necesidad de cambiar Gradle.
