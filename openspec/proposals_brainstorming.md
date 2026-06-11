# Bocatta POS — Lluvia de Ideas y Propuestas (Enfoque Simplificado)

Este documento centraliza los acuerdos tomados sobre las mejoras del sistema, priorizando la simplicidad del código, la velocidad en mostrador y evitando la complejidad innecesaria de infraestructura (arquitectura self-contained).

---

## 1. FLUJOS OPERATIVOS DEL EMPLEADO (VENDEDOR)

### 1.1. Optimización del Flujo de Cobro (Cambio y Billetes)
*   **Cómo está hoy:** Los botones de billetes ($100, $200, etc.) en `DialogosVentas.kt` sobrescriben el campo "Recibido con". Si el cliente paga con un billete de $200 y uno de $50, el cajero no puede sumarlos tocando ambos botones; tiene que usar el teclado virtual, lo cual ralentiza el mostrador.
*   **Cómo debería ser:** 
    *   **Billetes Acumulativos:** Al tocar los botones de billetes, estos se **suman** al monto actual de "paga con" (ej. tocar `$200` y luego `$50` ingresa automáticamente `$250.00`).
    *   **Botón de Efectivo Exacto Destacado:** Un botón grande y visible que cobre el total neto de un solo toque sin abrir más diálogos.
    *   **Teclado Integrado en Pantalla:** En lugar de levantar el teclado del sistema (que tapa la mitad de la pantalla en teléfonos y tablets pequeñas), tener un keypad numérico integrado en el propio diálogo de pago.

### 1.2. Fricción en el Cierre de Turno por Centavos
*   **Cómo está hoy:** Si la caja tiene una discrepancia mínima (incluso centavos o $1 peso), el empleado no puede cerrar su turno a menos que sea Administrador o que un Admin ingrese su PIN presencialmente.
*   **Cómo debería ser:** El sistema debe usar el parámetro `toleranciaEfectivo` (ej. +/- $10 pesos). Si la diferencia está dentro de la tolerancia, el empleado puede cerrar el turno de manera normal; la diferencia se registra automáticamente en la bitácora de auditoría para revisión de caja del Admin, sin detener la operación de relevo de turnos.

### 1.3. UI de Toppings en CrepeBuilderDialog (Evitar Scroll)
*   **Cómo está hoy:** Los toppings se despliegan en una lista larga. En plena hora pico, el cajero pierde segundos valiosos haciendo scroll buscando ingredientes.
*   **Cómo debería ser:** Agrupar toppings en pestañas o grids muy compactos de tamaño fijo (ej. Frutas, Chocolates, Salados) que se ajusten a la pantalla para que todo sea visible de un solo vistazo.

---

## 2. FLUJOS DE CONTROL DEL ADMINISTRADOR (IN-APP)

### 2.1. Gestión de Gastos y Compras Rápidas
*   **Cómo está hoy:** El flujo de "Compra Rápida" (compras de insumos de emergencia en Oxxo/Supermercado) está desacoplado del balance de efectivo de caja en tiempo real.
*   **Cómo debería ser:** Cada compra rápida autorizada y registrada desde la tablet debe descontarse automáticamente del flujo de efectivo activo de la caja del turno (`totalGastosSistema`), asegurando que el arqueo de caja al final del día cuadre perfectamente sin cuadres manuales externos.

### 2.2. Bitácora de Auditoría en Vivo (Pestaña "Auditoría")
*   **Cómo está hoy:** Los eventos de auditoría (cancelación de productos, mermas, cierres con diferencia) se suben a Firestore pero no son visibles para el encargado dentro de la app sin ir a la consola.
*   **Cómo debería ser:** En el módulo de Reportes integrado, añadir la sección de **Auditoría en Vivo**. El Admin/Encargado del turno puede ver en tiempo real una línea de tiempo con las acciones sensibles de los empleados (ej. *"15:10 - Cajero canceló Crepa Dulce (Motivo: Error de captura)"*).

### 2.3. Control de Autorizaciones Sin Bloquear Mostrador
*   **Cómo está hoy:** Si el vendedor necesita cancelar un producto o aplicar un descuento, la app se bloquea solicitando PIN de administrador presencial. Si el administrador no está físicamente al lado de la tablet, la fila de clientes se detiene.
*   **Cómo debería ser:** Implementar una regla de **"Registro de Alerta Directo"**: permitir que el vendedor realice la cancelación escribiendo el motivo, y en lugar de bloquear la pantalla pidiendo PIN, el sistema guarda el evento directamente en la bitácora de auditoría bajo el estado "Pendiente de Revisión por Admin" para que el administrador lo analice en su corte de caja, permitiendo que la venta continúe fluida en horas pico.

---

*Última actualización: Junio 2026.*
