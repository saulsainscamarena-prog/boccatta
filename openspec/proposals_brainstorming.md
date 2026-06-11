# Bocatta POS — Lluvia de Ideas y Propuestas (Enfoque Simplificado)

Este documento centraliza los acuerdos tomados sobre las mejoras del sistema, priorizando la simplicidad del código, la velocidad en mostrador y evitando la complejidad innecesaria de infraestructura (arquitectura self-contained).

---

## 1. MÓDULO DE VENTAS Y CONSUMO (APP)

### 1.1. Cobro Rápido con Billetes de México (Efectivo)
*   **Enfoque:**
    *   Tanto para tablets como para teléfonos, la interfaz de pago debe priorizar botones de denominación directa para billetes mexicanos ($20, $50, $100, $200, $500, $1000) y un botón de **Efectivo Exacto** muy accesible.
    *   Esto acelera el cobro físico en mostrador y evita errores de cálculo de cambio sin agregar complejidad técnica.

### 1.2. Gestión de Desechables (Sin Sobrecargar el Código)
*   **Decisión de Arquitectura:**
    *   **NO** agregar procesos asíncronos, listeners adicionales o corrutinas complejas en la base de datos para esto.
    *   Mantener el cálculo de empaques de forma síncrona en memoria en la capa de dominio (`InventoryDeductions.kt`) al momento de finalizar la venta.
    *   Permitir únicamente un ajuste básico de "Tipo de Venta" en el checkout para deducir el combo de desechables correspondiente, manteniendo el código limpio y libre de deudas técnicas.

---

## 2. PANEL DE ADMINISTRADOR INTEGRADO (DENTRO DE LA APP)

### 2.1. Catálogo, Mermas y Descuentos en la App
*   **Decisión de Arquitectura:**
    *   **NO** se implementará un portal web independiente para evitar costos de hosting, despliegue y mantenimiento de otra plataforma.
    *   Toda la administración (Gestión de precios por sucursal, mermas de producción, reportes de descuentos y auditoría) formará parte del módulo `feature:admin` **dentro de la propia aplicación Android**.
    *   Esto centraliza el desarrollo en una única base de código y aprovecha la persistencia local/remota que la app ya tiene construida.

---

## 3. SEGURIDAD Y ROBUSTEZ (INDISPENSABLES)

*   **Prevención de Doble Toque:** Flag en UI para bloquear el botón de confirmación de pago en cuanto se registra el primer click, previniendo duplicidad de ventas.
*   **Detección de Teléfono Duplicado en Clientes:** Al registrar un cliente con su Nombre y WhatsApp, alertar si el número ya existe en SQLite local o Firestore para evitar registros duplicados.

---

*Última actualización: Junio 2026.*
