# Bocatta POS — Lluvia de Ideas y Propuestas de Mejora (App-Level)

Este documento centraliza las propuestas aprobadas y las ideas en desarrollo para mejorar la operación del mostrador, la consistencia de inventario y la seguridad financiera de Bocatta POS, basándose en la arquitectura actual de la aplicación.

---

## 1. Tipo de Venta y Desechables Dinámicos (Consumibles)

### Contexto
Hoy en día, el sistema asume que todo consumo local no requiere empaques y que para llevar deduce consumibles de forma rígida en `InventoryDeductions.kt`. Dado que en la operación real **todo es desechable**, necesitamos mayor flexibilidad.

### Propuesta
- **Selector de Tipo de Venta (Modalidad):**
  - **Consumo Local:** Deducción base de plato/charola + tenedor + servilletas.
  - **Para Llevar:** Deducción base + empaques de transporte (cajas, bolsas de papel/plástico, domos).
  - **Plataformas (Uber/Didi/Rappi):** Deducción de empaques reforzados + sellos de seguridad (opcionalmente omitir cubiertos si el cliente no lo solicita).
- **Ajuste Manual de Consumibles:**
  - Permitir al cajero en el carrito desmarcar consumibles específicos (ej. *"Sin tenedor"*, *"Sin bolsa"*) para que no se descuenten de inventario, reflejando el ahorro real y reduciendo la merma teórica.

---

## 2. Smart Admin Screen (Módulo Unificado de Reportes y Auditoría)

### Contexto
Se busca evitar tener reportes y métricas dispersas en la aplicación ("cosas aquí y allá"). Se propone un diseño de pantalla inteligente ("Smart Screen") interactivo.

### Propuesta
- **Unificación de Datos:**
  - Un solo punto de entrada para el Administrador que unifique:
    - **Ventas y Utilidades** (Ingresos brutos, netos, promedio por ticket).
    - **Gastos del Turno** (Efectivo retirado, compras rápidas).
    - **Auditoría de Mermas por Empleado:** Listado de porciones/insumos desperdiciados hoy, quién los registró y bajo qué motivo (ej. "Se quemó", "Se cayó").
    - **Auditoría de Descuentos/Promociones:** Comparativa de pérdidas autorizadas por promociones del sistema versus descuentos manuales aplicados por el cajero.
- **Visualización:**
  - Tablas dinámicas y gráficas interactivas que permitan filtrar por turno, empleado o categoría en una sola vista consolidada.

---

## 3. Blindaje de Checkout contra "Doble Toque" (Double Tap Prevention)

### Contexto
En `DialogosVentas.kt`, el botón de confirmación de pago puede ser presionado dos veces antes de que cambie el estado en el hilo de UI, provocando duplicación de folios de venta y transacciones repetidas en la base de datos local y Firebase.

### Propuesta
- **Prevención en UI:**
  - Implementar un flag reactivo local en el composable del botón (`var isProcessing by remember { mutableStateOf(false) }`).
  - Al hacer click por primera vez, cambiar `isProcessing = true` de forma síncrona en el hilo principal antes de invocar la función del ViewModel.
  - Deshabilitar el botón de inmediato si `isProcessing || vmV2.cargando` es verdadero.

---

## 4. Validación de Clientes y Prevención de Duplicados

### Contexto
El sistema de lealtad se basa únicamente en el **Nombre** y **Teléfono (WhatsApp)** del cliente. Al registrar un nuevo cliente, no se verifica si el teléfono ya existe, lo que permite duplicados y registros inconsistentes.

### Propuesta
- **Validación en Tiempo Real:**
  - En `RegistrarClienteDialog.kt`, mientras el cajero escribe los 10 dígitos del teléfono, realizar una consulta rápida en background (`customerRepository.buscarCliente`).
  - Si el número ya existe, deshabilitar el botón "Registrar Cliente" y mostrar una advertencia visual: *"Este teléfono ya está registrado a nombre de [Nombre]. ¿Querés seleccionarlo?"* con un botón para asociarlo de inmediato en vez de crear otro.

---

*Guardado para referencia de desarrollo futuro en Bocatta POS. Fecha de creación: Junio 2026.*
