# 🥯 Bocatta POS - Ecosistema Industrial V2

Este documento detalla la arquitectura de la Versión 2 (Industrial) de Bocatta POS, diseñada para escalabilidad multisucursal y consistencia de datos.

## 🏗️ Arquitectura de Datos (Firebase Firestore)

Todas las colecciones industriales utilizan el prefijo `v2_` para garantizar aislamiento del sistema legado.

### Colecciones Principales:
- `v2_products`: Catálogo maestro de productos. Los precios se almacenan en un mapa `precioVenta: Map<String, Double>` donde la llave es el nombre de la sucursal (ej: "Metepec").
- `v2_ventas`: Registro transaccional atómico. Cada documento incluye `total`, `atendio`, `sucursal` y `metodoPago`.
- `v2_inventory_global`: Bodega central (Materia Prima).
- `v2_inventory_branch_portions`: Stock local de venta por sucursal.
- `v2_auditoria_cancelaciones`: Bitácora de seguridad que registra cada ítem eliminado del carrito (Anti-Fraude).

## 🧪 Lógica de Negocio (Industrialización)

1. **Recetas de Producción**: Los productos elaborados (Masa, Porciones) se descuentan de la materia prima global mediante un factor de conversión definido en `RecetaTandaV2`.
2. **Cierre Atómico**: El cierre de caja actualiza simultáneamente el estado de la sucursal (`v2_config`) y el historial financiero.
3. **Seguridad de Operación**:
   - Fondo mínimo de apertura: $100.00.
   - Límite de producción: 1200 unidades/tanda (Protección contra errores de dedo).
   - Doble Confirmación: Botones críticos (Borrar Todo, Cierre de Turno) requieren confirmación visual.

## 📱 Estándares de Código (Senior Android)
- **ViewVModels**: Usan `safeHandler` (CoroutineExceptionHandler) para evitar cierres inesperados.
- **UI**: Jetpack Compose con estados inmutables y optimización de recomposición mediante llaves en LazyLists.
- **Offline**: Persistencia local activada para operar en zonas de baja conectividad.

## 🏆 Certificación Golden Master (Audit Final 360)
- **Seguridad**: Cero claves hardcodeadas. Validación dinámica vía Firestore `configuracion/seguridad`.
- **Resiliencia**: Sistema de "Caja Negra" para recuperación de tickets digitales ante fallos de red.
- **Precisión**: Inventario inteligente basado en modelos con vinculación de stock atómica.
- **Infraestructura**: Manual de índices críticos disponible en `INDEXES.md` para escalabilidad masiva.

---
*Desarrollado con rigor de ingeniería para Bocatta por Antigravity (Advanced Agentic Coding).*
*Versión: 2.0.0-GOLDEN_MASTER (Abril 2026)*
