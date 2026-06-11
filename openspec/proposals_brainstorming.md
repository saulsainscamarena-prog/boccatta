# Bocatta POS — Lluvia de Ideas, Diagnósticos y Propuestas (App & Web)

Este documento centraliza el diagnóstico técnico-operativo de Bocatta POS y las propuestas de desarrollo tanto para la aplicación de mostrador (Android) como para el futuro Panel Web de Administración (Dueño).

---

## PARTE 1: DIAGNÓSTICO Y PROPUESTAS PARA LA APP (TABLET/MÓVIL)

### 1.1. Diagnóstico de la App Actual
1.  **Falta de validación de entradas financieras:** Al ingresar efectivo recibido o conteos de caja, el sistema depende de que el cajero haga el cálculo mental del cambio o del arqueo total. Esto genera errores operativos.
2.  **Búsqueda rígida en el catálogo:** La barra de búsqueda de `SalesScreen.kt` filtra por coincidencia exacta de texto (`.contains`). Un error de dedo (ej. *"crepa salda"*) arroja cero resultados en plena venta.
3.  **Falta de visibilidad del estado de colas offline:** El cajero no sabe con certeza si las ventas se guardaron en SQLite local o si ya se sincronizaron con Firestore, lo que genera desconfianza ante cortes de red.

### 1.2. Propuestas de Mejora para la App
*   **Calculadora de Denominaciones en Caja (Arqueo):**
    *   En lugar de un campo de texto plano para ingresar el monto del arqueo, mostrar un formulario dinámico donde el cajero ingrese las cantidades de billetes/monedas (ej. 5 billetes de $500, 10 de $200). La app calcula la suma final automáticamente, eliminando errores de cálculo manual.
*   **Búsqueda Difusa (Fuzzy Search) en Mostrador:**
    *   Implementar el algoritmo de Levenshtein en el filtrado de productos de `SalesScreen.kt` (como ya se hace en clientes). Si el cajero escribe *"wafel de oreo"*, el catálogo debe seguir mostrando *"Waffle Oreo"*.
*   **Monitor Visual de Sincronización Offline (Widget de Barra):**
    *   Añadir un badge discreto en la barra superior junto al estado de red. Por ejemplo: `[Sync: 3 pendientes]`. Si el sistema está offline y hay 3 ventas en SQLite local pendientes de subir por `SyncWorker`, el cajero tiene la tranquilidad de que sus tickets están a salvo.
*   **Selector Dinámico de Desechables y Modalidad:**
    *   Reemplazar el switch de "Para Llevar" por un selector de tipo de venta (Local, Llevar, Delivery). El sistema deducirá de forma inteligente los insumos asociados (platos, cubiertos, bolsas, cajas).

---

## PARTE 2: DIAGNÓSTICO Y PROPUESTAS PARA LA WEB (PANEL DE DUEÑO)

### 2.1. Diagnóstico del Flujo Administrativo
1.  **Dependencia de la tablet para administración:** Modificar precios, crear insumos o revisar recetas hoy en día se hace desde el módulo de administración en la tablet de ventas. Esto interrumpe la operación diaria del mostrador y es incómodo para pantallas táctiles.
2.  **Carencia de métricas consolidadas en tiempo real:** El dueño no tiene un panel remoto para comparar las ventas, gastos y rendimientos de la sucursal Atlixco contra la sucursal Metepec en vivo, teniendo que revisar Firestore a mano o depender de reportes compartidos por WhatsApp.

### 2.2. Propuestas de Mejora para la Web (Portal del Dueño)
*   **Catálogo Centralizado Multi-Sucursal:**
    *   Una aplicación web ligera (React/Next.js) conectada al mismo Firestore. El dueño puede crear productos, editar recetas y modificar precios de venta diferenciados por sucursal (Metepec/Atlixco) cómodamente desde su computadora. Las tablets reciben las actualizaciones en vivo gracias a los listeners de Firestore.
*   **Panel Consolidador de Ventas y Gastos en Tiempo Real:**
    *   Gráficas interactivas que comparen el rendimiento de ambas sucursales. Muestra curvas de ventas por hora, ticket promedio y los productos más vendidos en el día de forma remota.
*   **Consola de Administración de Mermas por Empleado:**
    *   Un reporte consolidado web que agrupe las mermas registradas por los encargados en cada tablet, permitiendo filtrar por sucursal, empleado y tipo de merma para analizar pérdidas de materia prima.
*   **Editor de Reglas de Promociones Dinámicas:**
    *   Interfaz visual para crear objetos `PromocionUniversal` (BOGO 2x1, Happy Hours por día/hora, cupones de descuento). Una vez guardados en la web, se despliegan automáticamente a todas las sucursales sin necesidad de tocar código.
*   **Gestor de Empleados y Nómina Operativa:**
    *   Administrar el personal, asignar roles (Admin, Vendedor), generar nuevos PINs de caja y configurar salarios base para control interno de la nómina.

---

*Actualizado y expandido en el repositorio del proyecto. Fecha: Junio 2026.*
