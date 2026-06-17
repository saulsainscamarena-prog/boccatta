# SaaS Multi-Giro

Spec ID: `saas-multi-giro-1`
Capability: `Feature Toggling & Multi-Tenant Architecture`
Fecha: `2026-06-12`

## Problema

El sistema actual de Bocatta asume que todos los clientes son creperías (Restaurantes). Al vender licencias a otros tipos de negocios (Retail, Tiendas de Ropa, Abarrotes), los flujos de "Armar Producto" (Crepe Builder / Constructores) estorban la agilidad de la venta. Se requiere un sistema de configuración en tiempo de ejecución para saltarse flujos específicos según el giro comercial del negocio.

## Objetivos

- Proveer un mecanismo basado en el tenant actual (`RETAIL` o `RESTAURANT`) para habilitar/deshabilitar constructores.
- Implementar `BusinessFeatures` y proveerlo en el `SalesScreen` para bypassear lógicas duras (hardcoded).
- Sentar las bases para escalabilidad SaaS.

## No Objetivos

- Migrar todo el backend de Firebase a un modelo verdaderamente multi-tenant por colección en esta iteración.
- Soporte para giros comerciales no documentados todavía.

## Usuarios y Flujos

### Flujo principal (Retail)

1. El usuario escanea o selecciona un producto.
2. Si la feature `providesCrepeBuilder()` es falsa (giro Retail), el sistema inserta el producto directo al carrito con precio base, sin abrir modales.

### Flujo principal (Restaurante)

1. El usuario selecciona una crepa o producto compuesto.
2. La feature `providesCrepeBuilder()` es verdadera.
3. Se abre el flujo interactivo de bases y toppings.

## Requisitos Funcionales

- R1: El sistema debe leer el `businessType` desde la sesión (`TenantSessionManager`).
- R2: El `BusinessLogicProvider` debe implementar la lógica de toggling.
- R3: `SalesScreen` debe reaccionar sin recomposiciones forzadas.

## Requisitos No Funcionales

- Rendimiento de mostrador: El bypass debe ahorrar tiempo de click y renderizado.

## Criterios de Aceptacion

- [x] La sesión expone el giro comercial.
- [x] El proveedor de features inyecta el estado `providesCrepeBuilder()`.
- [x] Retail no abre modales de crepa.
- [x] Restaurante abre modales normalmente.

## Riesgos POS

- Ventas: Si el toggle falla, un usuario retail podría quedar atorado en modales que no puede llenar.

## Preguntas Abiertas

- ¿Cómo definiremos futuros giros (ej. Servicios)? (Se abordará en iteraciones posteriores).
