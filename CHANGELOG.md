# Changelog – Bocatta POS

## v2.0.0-stable (2026-05-13)

### 🎨 Diseño y Tema
- Migración completa a **Material Design 3** (Color, Typography, Shapes)
- Colores dinámicos (Material You) con fallback para versiones anteriores
- Todos los colores hardcodeados reemplazados por `MaterialTheme.colorScheme.*`
- 31 archivos UI migrados al sistema de temas M3

### 📱 Pantallas principales
- **SalesScreen**: Barra de búsqueda, categorías, grid de productos, carrito responsivo
- **CrepeBuilderDialog**: Configurador con aderezos y toppings multiselección
- **PagoDialog**: Propina, cambio rápido, pago mixto, nota de orden
- **CarritoPanelV2**: Descuento manual, editar ítem, undo
- **AdminScreen**: Gestión de productos, insumos, promociones, auditoría

### 💳 Flujo de pago
- **Propina**: Chips 10%/15%/20% y monto personalizado
- **Cambio rápido**: Denominaciones $20/$50/$100/$200/$500/$1000
- **Pago mixto**: Distribuir el total entre efectivo, tarjeta y transferencia
- **Nota de orden**: Campo de texto para instrucciones del cliente
- **Descuento manual**: Aplicar 10%/20%/30% sobre el subtotal

### 🛠️ Control de errores
- **Undo (Deshacer)**: Restaurar el carrito al estado anterior tras agregar/eliminar
- **Editar ítem**: Botón de lápiz en cada fila del carrito que reabre el configurador
- **Cancelar venta con PIN**: Diálogo de autorización admin para vaciar el carrito

### 🔒 Seguridad y Calidad
- `enableEdgeToEdge()` configurado correctamente
- R8: Reglas redundantes eliminadas (Firebase, Google Play, Kotlin, Koin)
- `proguard-rules.pro` optimizado
- 218 tests unitarios, 0 fallos

### 📦 Técnico
- AGP 9.2.1, SDK 36, Compose BOM 2026.02.01
- Kotlin 2.2.10, material3 >=1.4.0
- Compilación release con R8 full mode

---

### Próximo ciclo – v2.1.0 (Planificado)
| Funcionalidad | Prioridad |
|---|---|
| Apartar / Hold orden | 🔴 Alta |
| Atajos de teclado / escáner | 🟡 Media |
| Dividir cuenta (split bill) | 🟢 Baja |
