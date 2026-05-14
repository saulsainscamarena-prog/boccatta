# Bocatta POS — Plan de Desarrollo v2.3+

## Prioridades ordenadas por impacto comercial

---

### Fase 1 — Presentaciones múltiples de compra
**Esfuerzo:** 1.5 semanas | **Impacto:** 🟡 Alto (control de inventario)

**Problema:** Los insumos se compran en diferentes presentaciones (caja de 21 paquetes, paquete individual, bolsa por kg) pero el sistema actual solo registra unidades simples.

**Solución:**
| Capa | Archivo | Cambio |
|------|---------|--------|
| Modelo | `domain/model/PresentacionCompra.kt` | 🆕 Data class con `insumoId`, `nombre`, `contenido`, `unidadBase` |
| Repositorio | `IInventoryRepository.kt` | + `registrarCompraConPresentacion(insumoId, presentacion, cantidad, costoTotal)` |
| BD | `OfflineDatabase.kt` | Tabla `presentaciones_compra` (ya existe `TABLE_PRESENTACIONES`) |
| UI | `ProductionRegistrationDialog.kt` | + Modo "COMPRA" con selector de presentación + cálculo automático de porciones |
| Seeder | `FirestoreSeeder.kt` | Agregar presentaciones para Oreo (caja 21×14), Marías (paquete 3×170g), Mexicanas (paquete 5×135g) |

---

### Fase 2 — Split Bill (dividir cuenta)
**Esfuerzo:** 2 semanas | **Impacto:** 🟡 Alto (grupos grandes, ventas B2B)

**Problema:** Grupos de clientes no pueden dividir la cuenta entre varias personas con diferentes métodos de pago.

**Solución (versión simple — dividir por partes iguales):**
| Capa | Archivo | Cambio |
|------|---------|--------|
| Modelo | `domain/model/SplitPayment.kt` | 🆕 Data class con lista de `(items, metodo, monto)` |
| ViewModel | `SalesViewModelV2.kt` | + `splitMode`, `splitCount`, `splitPayments`, método `finalizarSplitVenta()` |
| UI | `PagoDialog.kt` | + Botón "Dividir cuenta" + campos cantidad de personas |
| UI | `SplitPaymentDialog.kt` | 🆕 Diálogo para asignar items por persona + método de pago por grupo |

**Reglas de negocio:**
- División por partes iguales (50/50, 33/33/34, 25/25/25/25)
- Cada grupo elige su método de pago
- Redondeo automático con `BigDecimal.ROUND_HALF_UP` (el último grupo absorbe los centavos sobrantes)
- Se generan N sub-órdenes con mismo `parentOrderId`

---

### Fase 3 — UI Tests (Compose Test)
**Esfuerzo:** 1 semana | **Impacto:** 🟢 Medio (calidad)

**Problema:** Sin tests instrumentados, cada cambio requiere probar manualmente toda la app.

**Flujos a testear:**
| Test | Descripción |
|------|-------------|
| `AgregarProductoTest` | Tocar producto → ver configurador → confirmar → ver en carrito |
| `DescuentoManualTest` | Agregar items → aplicar 20% → ver total actualizado |
| `UndoTest` | Agregar → ver snackbar → tocar "DESHACER" → carrito vacío |
| `PagoMixtoTest` | Abrir pago → activar mixto → asignar montos → confirmar |
| `HoldOrderTest` | Agregar → apartar → ver en lista → recuperar |
| `PropinaTest` | Abrir pago → tocar 15% → ver total con propina |

**Stack:** `androidx.compose.ui.test.junit4.ComposeTestRule` + `ComposeContentTestRule`

---

### Fase 4 — Atajos de teclado y escáner de código de barras
**Esfuerzo:** 3 días | **Impacto:** 🟡 Medio (eficiencia operativa)

**Problema:** Los operadores avanzados usan atajos de teclado y escáneres de código de barras, pero la app no los soporta.

**Solución:**
| Elemento | Archivo | Cambio |
|----------|---------|--------|
| Atajos | `SalesScreen.kt` | `Modifier.onKeyEvent` captura F1(🔍), F2(💳), F3(👤), ESC(❌) |
| Escáner | `SalesScreen.kt` | `TextField` oculto con `focusRequester` que captura input continuo y busca producto por ID |
| ViewModel | `SalesViewModelV2.kt` | + `buscarProductoPorCodigo(codigo: String): SalesInventoryProductV2?` |

---

### Fase 5 — Migración a Room (condicionada a KSP)
**Esfuerzo:** Variable | **Impacto:** 🟢 Medio (mantenibilidad)

**Dependencia:** Requiere que KSP publique una versión compatible con Kotlin 2.2.x.

| Archivo | Cambio |
|---------|--------|
| `build.gradle.kts` | Habilitar KSP, agregar dependencia Room |
| `OfflineDatabase.kt` | Reemplazar `SQLiteOpenHelper` por `@Database` Room |
| `InventarioRepository.kt` | Reemplazar consultas SQL por DAOs |
| `TECH_DEBT.md` | Marcar deuda como resuelta |

---

### 📐 Estimación total

| Fase | Duración | Dependencias |
|------|----------|-------------|
| F1 — Presentaciones compra | 1.5 semanas | Ninguna |
| F2 — Split Bill | 2 semanas | Ninguna |
| F3 — UI Tests | 1 semana | F1, F2 (para testear esas features) |
| F4 — Atajos/escáner | 3 días | Ninguna |
| F5 — Room | Variable | KSP compatible |

**Total estimado:** ~5 semanas de desarrollo secuencial.
