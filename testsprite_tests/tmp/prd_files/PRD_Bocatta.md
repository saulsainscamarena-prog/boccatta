# Bocatta POS - Requisitos del Producto y Documentación de Pruebas

## 📱 Descripción de la Aplicación

**Bocatta POS** es una aplicación Android de punto de venta (POS) diseñada para restaurantes y pequeños comercios. Permite gestionar ventas, inventario, caja, clientes y reportes en tiempo real con soporte offline y sincronización con Firebase Firestore.

---

## 🏢 Funcionalidades Principales

### 1️⃣ Autenticación y Sesión
- Login con email/contraseña
- Roles: ADMIN, DUEÑO, VENDEDOR
- Sesión persistente
- Cierre de sesión seguro

### 2️⃣ Apertura del Día
- Selección de sucursal
- Configuración de caja/turno
- Registro de efectivo inicial

### 3️⃣ Módulo de Ventas (核心功能)
- Catálogo de productos searchable
- Carrito de compras
- Ajuste de cantidades
- Aplicar descuentos (%)
- Cálculo automático de totales
- Procesamiento en efectivo
- Generación de tickets

### 4️⃣ Gestión de Inventario
- Vista por categoría
- Stock en tiempo real
- Movimientos de entrada/salida
- Ajuste de inventario
- Apertura/cierre de inventario
- Sincronización Firestore

### 5️⃣ Gestión de Caja
- Apertura de caja
- Registro de ventas
- Cierre con resumen diario

### 6️⃣ Gastos
- Registro por categoría
- Historial por sucursal

### 7️⃣ Devoluciones
- Procesamiento con razón
- Actualización automática de inventario

### 8️⃣ Clientes
- Registro de clientes

### 9️⃣ Módulo Admin (solo ADMIN/DUEÑO)
- Configuración de negocio
- Gestión de sucursales
- Dashboard de bodega
- Reportes de inventario
- Sincronización

### 🔟 Reportes
- Ventas por período
- Productos más vendidos
- Ganancias y pérdidas

---

## 👥 Roles de Usuario

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **ADMIN** | Administrador total | Config, usuarios, reportes |
| **DUEÑO** | Propietario | Acceso total a su sucursal |
| **VENDEDOR** | Vendedor | Ventas, inventario básico |

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 1.9.x
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** MVVM + Clean Architecture
- **Backend:** Firebase Firestore (offline-first)
- **DI:** Koin 4.0.0
- **Navegación:** Navigation Compose
- **Target SDK:** 36
- **Min SDK:** 24

---

## 🎯 Casos de Prueba para TestSprite

### ✅ Autenticación

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC001 | Login con credenciales válidas | 1. Lanzar app 2. Ingresar admin@bocatta.com / admin123 3. Clic en Iniciar Sesión | Navega a pantalla de inicio del día |
| TC002 | Login con credenciales inválidas | 1. Lanzar app 2. Ingresar email inválido 3. Clic en Iniciar Sesión | Muestra mensaje de error |
| TC003 | Logout | 1. Estar logueado 2. Clic en Cerrar Sesión | Regresa a pantalla de login |

### ✅ Apertura del Día

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC010 | Seleccionar sucursal | 1. En pantalla de inicio 2. Seleccionar sucursal | Sucursal selecciona correctamente |
| TC011 | Abrir caja | 1. Ingresar monto inicial 2. Clic en Abrir Caja | Cajaabierta, navega a ventas |

### ✅ Proceso de Ventas (核心)

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC020 | Ver catálogo de productos | 1. En pantalla de ventas 2. Ver lista de productos | Productos se cargan |
| TC021 | Buscar producto | 1. En ventas 2. Ingresar nombre en búsqueda | Resultados filtrados |
| TC022 | Agregar producto al carrito | 1. Clic en + de un producto | Contador del carrito aumenta |
| TC023 | Ajustar cantidad | 1. En carrito 2. Cambiar cantidad | Total se actualiza |
| TC024 | Aplicar descuento | 1. Ingresar % de descuento 2. Aplicar | Descuento aplicado al total |
| TC025 | Cobrar | 1. Clic en Cobrar 2. Ingresar monto recibido 3. Confirmar | Cambio calculado, ticket generado |
| TC026 | Ver ticket | 1. Después de venta 2. Ver ticket | Ticket muestra detalles |

### ✅ Inventario

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC030 | Ver lista de inventario | 1. Navegar a Inventario | Lista de productos visible |
| TC031 | Buscar en inventario | 1. Ingresar término de búsqueda | Resultados filtrados |
| TC032 | Ajustar stock | 1. Seleccionar producto 2. Cambiar cantidad 3. Guardar | Stock actualizado en Firestore |

### ✅ Caja

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC040 | Apertura de caja | 1. Ir a caja 2. Ingresar monto 3. Abrir | Cajaabierta |
| TC041 | Ver transacciones | 1. En caja 2. Ver historial | Lista de transacciones visible |
| TC042 | Cierre de caja | 1. Clic en Cerrar Caja 2. Confirmar | Resumen generado, cierre registrado |

### ✅ Gastos

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC050 | Registrar gasto | 1. Ir a Gastos 2. Nuevo gasto 3. Ingresar monto y categoría | Gasto registrado |
| TC051 | Ver historial de gastos | 1. En gastos 2. Ver lista | Historial visible |

### ✅ Navegación

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC060 | Navegar a inventario desde ventas | 1. En ventas 2. Clic en Inventario | Navega correctamente |
| TC061 | Navegar a reportes (admin) | 1. Como admin 2. Ir a Admin 3. Reportes | Reportes visibles |
| TC062 | Botón atrás | 1. En cualquier pantalla 2. Clic atrás | Regresa a pantalla anterior |

### ✅ Admin

| ID | Descripción | Pasos | Resultado Esperado |
|----|------------|-------|-----------------|
| TC070 | Acceso denegado sin rol admin | 1. Como vendedor 2. Ir a Admin | Acceso denegado, regresa |
| TC071 | Gestión de sucursales | 1. Como admin 2. Ir a Sucursales | CRUD funcional |

---

## 🔑 Credenciales de Prueba

| Email | Contraseña | Rol |
|-------|-----------|-----|
| admin@bocatta.com | admin123 | ADMIN |
| dueuno@bocatta.com | due123 | DUEÑO |
| vendedor@bocatta.com | vend123 | VENDEDOR |

---

## 📱 Dispositivo de Prueba

- **Emulador:** Pixel 8 API 35
- **Dispositivo físico:** Android 12+ (API 31+)
- **RAM:** Mínimo 4GB
- **Almacenamiento:** 500MB libres

---

## ✅ Criterios de Éxito

- ✅ Al menos **80%** de casos de prueba pasan
- ⏱️ Tiempo de ejecución < 10 minutos
- 🚫 Sin crashes durante las pruebas
- 🔄 Navegación fluida entre pantallas

---

## 📋 Checklist de Pruebas Prioritarias

| Prioridad | Caso de Prueba | Requisito |
|----------|---------------|----------|
| 🔴 ALTA | Login exitoso | Autenticación |
| 🔴 ALTA | Login fallido muestra error | Autenticación |
| 🔴 ALTA | Agregar producto alcarrito | Ventas |
| 🔴 ALTA | Cobrar y generar ticket | Ventas |
| 🔴 ALTA | Cierre de caja | Caja |
| 🟡 MEDIA | Navegación a inventario | Navegación |
| 🟡 MEDIA | Búsqueda de productos | Inventario |
| 🟡 MEDIA | Ajuste de stock | Inventario |
| 🟡 MEDIA | Registro de gasto | Gastos |
| 🟢 BAJA | Ver reportes | Reportes |
| 🟢 BAJA | Gestión de clientes | Clientes |

---

*Documento generado para TestSprite - Mayo 2026*