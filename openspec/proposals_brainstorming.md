# Bocatta POS — Lluvia de Ideas y Propuestas (Enfoque Simplificado)

Este documento centraliza los acuerdos tomados sobre las mejoras del sistema, priorizando la simplicidad del código, la velocidad en mostrador y evitando la complejidad innecesaria de infraestructura (arquitectura self-contained).

---

## 1. FLUJOS OPERATIVOS DEL EMPLEADO (VENDEDOR)

### 1.1. Optimización del Flujo de Cobro (Cambio, Billetes y Monedas)
*   **Cómo está hoy:** Los botones de billetes ($100, $200, etc.) en `DialogosVentas.kt` sobrescriben el campo "Recibido con". Si el cliente paga con un billete de $200 y uno de $50, el cajero no puede sumarlos tocando ambos botones; tiene que usar el teclado virtual. Tampoco hay una forma rápida de ingresar impares ($45, $55) o cambio exacto en monedas.
*   **Cómo debería ser:**
    *   **Billetes Acumulativos:** Al tocar los botones de billetes, estos se **suman** al monto de "paga con" (ej. tocar `$200` y luego `$50` ingresa automáticamente `$250.00`).
    *   **Botón de Corregir (C):** Un botón pequeño y rápido `"C"` o `"Limpiar"` junto al campo "Recibido con" que regrese la cantidad a `$0.00` al instante para corregir de inmediato.
    *   **Atajos Dinámicos de Pago (Efectivo, Monedas e Impares):**
        *   Para evitar atascar la pantalla con botones individuales para cada moneda ($1, $2, $5, $10) que llenan la interfaz, el sistema generará de forma dinámica botones de pago basados en el total neto:
            1.  `[ Exacto: $XX ]` (ej. si el total es $45 o $55, cobra exactamente eso).
            2.  `[ Siguiente múltiplo de $5 ]` (ej. si el total es $42, muestra `$45`; si es $52, muestra `$55`).
            3.  `[ Siguiente múltiplo de $10 ]` (ej. si el total es $41, muestra `$50`; si es $56, muestra `$60`).
            4.  `[ Billetes superiores comunes ]` (ej. si el total es $70, muestra `$100`, `$200`).
        *   Esto permite cobrar montos impares y monedas con un solo toque, manteniendo la pantalla limpia.
    *   **Botón de Incremento Rápido en Monedas:** Botones discretos de suma `[ +$10 ]`, `[ +$5 ]` y `[ +$1 ]` para agregar cambio en monedas rápidamente de forma acumulativa.

### 1.2. Fricción en el Cierre de Turno por Centavos
*   **Nota de Auditoría:** La tolerancia de caja (`toleranciaEfectivo` y `toleranciaTarjeta`) **ya existe en el código** del ViewModel y se evalúa a través de `resultadoCuadreActual.esCorrecto`. Se mantendrá esta regla síncrona sin modificar.

### 1.3. UI de Toppings en CrepeBuilderDialog (Evitar Scroll)
*   **Enfoque:** Agrupar toppings en pestañas o grids muy compactos de tamaño fijo (ej. Frutas, Chocolates, Salados) que se ajusten a la pantalla para que todo sea visible de un solo vistazo.

---

## 2. FLUJOS DE CONTROL Y SEGURIDAD (ADMIN Y EMPLEADO)

### 2.1. Gestión de NIP (PIN) por el Propio Empleado (Autoservicio)
*   **Cómo está hoy:** El administrador registra o edita los NIPs desde la ficha del empleado (`DialogEmpleado.kt`), pero el empleado no tiene una pantalla propia donde configurar o cambiar su NIP de forma segura.
*   **Cómo debería ser:**
    *   **Flujo de Creación en Primer Login:**
        *   Cuando un nuevo empleado inicia sesión por primera vez con su cuenta de Firebase Auth (email/password temporal creado por el Admin), la app detectará que no tiene NIP activo en `v2_pin_authorizations`.
        *   Se le redirigirá obligatoriamente a una pantalla de configuración inicial de NIP de 4 dígitos (Ingresar NIP -> Confirmar NIP).
        *   El NIP se encripta y se guarda de forma segura.
    *   **Pantalla/Diálogo "Mi Perfil" (Cambiar NIP):**
        *   Opción en el menú lateral (Drawer) llamada "Mi Perfil" para el empleado activo.
        *   Permite cambiar el NIP ingresando el NIP anterior como validación de seguridad.
        *   *Beneficio:* El administrador nunca conoce los NIPs privados de los empleados, aumentando la seguridad y evitando confusiones o fraudes.

### 2.2. Gestión de Gastos y Compras Rápidas
*   **Enfoque:** Cada compra rápida autorizada y registrada desde la tablet por el empleado (cuando el admin no está) debe estar firmada con su propio NIP. Esto asocia el gasto a su ID de empleado y se deduce automáticamente de la caja activa (`totalGastosSistema`), previniendo confusiones y descuadres al final del turno.

### 2.3. Control de Autorizaciones Sin Bloquear Mostrador
*   **Enfoque:** Permitir cancelaciones ingresando el motivo en texto plano, guardando la alerta bajo el estado "Pendiente de Revisión por Admin" para revisión diferida de caja, evitando detener la venta en horas pico.

---

*Última actualización: Junio 2026.*
