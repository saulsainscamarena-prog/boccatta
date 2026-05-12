# Bocatta POS - Flujo de Inventario V2

## Arquitectura de Inventario

```
COMPRA → PRODUCCIÓN → STOCK SUCURSAL → VENTA → DESCUENTO
(Materias Primas)  (Tandas)     (Porciones)     (POS)
```

---

## 1. Materias Primas (v2_inventory_global)

Se Registran al comprar:
- Harina, Leche, Mantequilla, Huevos, Polvo Hornear, Bicarbonato
- Nutella, Queso Crema, Lechera
- Queso Mozzarella, Jamón, Peperoni, Chorizo
- Crema Batir, Azúcar Glass, Queso Crema, Fresas, Duraznos
- Boneless, Nuggets, Papas
- Charolas, Tenedores, Servilletas, Vasos, Domos, Papel Hamburguesero
- Galletas Oreo, Cocoa, Café, Galletas María, etc.

---

## 2. Producción (Admin registra tanda)

El Admin registra una tanda → convierte materias primas en porciones.

### Recetas de Producción (v2_recipes_production)

| ID | Nombre | Ingredientes por Tanda | Rendimiento |
|---|---|---|---|
| masa_crepa | Harina 2kg, Leche 3L, Mantequilla 125g, Huevos 14, Polvo 5g, Bicarbonato 5g | 60 porciones |
| carlota_unidad | Leche Evaporada 365g, Lechera 375g, Media Crema 225g, Jugo Limón 150g, Galletas María 6 pz | 4-5 porciones |
| fresas_crema_unidad | Crema Batir 500ml, Azúcar Glass 40g, Queso Crema 45g, Fresas 400g | 4-5 porciones |
| duraznos_crema_unidad | Crema Batir 500ml, Azúcar Glass 40g, Queso Crema 45g, Duraznos 400g | 4-5 porciones |
| tiramisu_unidad | Queso Crema 180g, Azúcar 160g, Media Crema 125g, Crema Batir 100g, Café 375ml, Galletas María 6 pz | 4-5 porciones |

**Resultado**: Se descuenta de global y suma a branch stock (porciones disponibles).

---

## 3. Stock en Sucursal (v2_inventory_branch_portions)

| stockId | Descripción | Unidad |
|---|---|---|
| masa_crepa | Porciones de masa prepared | Porción |
| carlota_unidad | Porciones de Carlota | Pieza |
| fresas_crema_unidad | Porciones de Fresas con Crema | Pieza |
| duraznos_crema_unidad | Porciones de Duraznos con Crema | Pieza |
| tiramisu_unidad | Porciones de Tiramisú | Pieza |
| boneless | Porciones Boneless (250g) | Kg |
| nuggets | Porciones Nuggets (~200g) | Kg |
| papitas | Porciones Papas (200g) | Kg |

---

## 4. Productos y Precios

### Crepas

| ID | Nombre | Atlixco | Metepec | stockId | factor |
|---|---|---|---|---|---|
| cr_dulce | Crepa Individual Dulce | $25 | $25 | masa_crepa | 1.0 |
| cr_salada | Crepa Individual Salada | $35 | $35 | masa_crepa | 1.0 |
| combo_2d | Combo 2 Crepas Dulces | $50 | $40 | masa_crepa | 2.0 |
| combo_2s | Combo 2 Crepas Saladas | $70 | $60 | masa_crepa | 2.0 |
| combo_duo | Combo Dulce y Salada | $65 | $55 | masa_crepa | 2.0 |

### Postres

| ID | Nombre | Atlixco | Metepec | stockId | factor |
|---|---|---|---|---|---|
| p_carlota | Carlota de Limón | $40 | $40 | carlota_unidad | 1.0 |
| p_tiramisu | Tiramisú | $45 | $45 | tiramisu_unidad | 1.0 |
| p_fresas | Fresas con Crema | $50 | $50 | fresas_crema_unidad | 1.0 |
| p_duraznos | Duraznos con Crema | $50 | $50 | duraznos_crema_unidad | 1.0 |

### Snacks

| ID | Nombre | Atlixco | Metepec | stockId | factor |
|---|---|---|---|---|
| s_papas_senc | Papas Fritas | $35 | $35 | papitas | 0.200 |
| s_papas_chor | Papas con Chorizo | $60 | $60 | papitas | 0.325 |
| s_boneless | Boneless | $90 | $90 | boneless | 0.250 |
| s_nuggets | Nuggets | $80 | $70 | nuggets | 0.200 |

### Frappes

| ID | Nombre | Atlixco | Metepec | stockId | factor |
|---|---|---|---|---|
| frappe_oreo | Frappe Oreo | $35 | $35 | nutella_kg | 0.020 |
| frappe_cocoa | Frappe Cocoa | $30 | $30 | nutella_kg | 0.020 |
| frappe_fresa | Frappe Fresa | $30 | $30 | nutella_kg | 0.020 |
| frappe_fresa_cocoa | Frappe Fresa Cocoa | $30 | $30 | nutella_kg | 0.030 |

---

## 5. Consumo de Venta (Descuentos automática)

| Categoría | Consumibles Descontados |
|---|---|
| Crepas/Combos | Charola, Tenedor, Servilleta, Papel Hamburguesero |
| Snacks | Charola, Tenedor, Servilleta, Papel Hamburguesero |
| Postres | Vaso, Domo, Tenedor, Servilleta |
| Frappes | Vaso, Domo, Servilleta |

### Reglas Especiales

- **Combo 2x Separadas**: Descuenta 2 de stock (esSeparado = true)
- **Combo 2x Juntas**: Descuenta 1 de stock (esSeparado = false)
- **Boneless/Nuggets**: Incluyen papas (-200g automáticamente)

---

## 6. Ejemplo de Venta Real

### 1. Admin registra compra (global)
```
Harina 2kg, Leche 3L, etc... → v2_inventory_global
```

### 2. Admin registra tanda producción
```
Masa crepa: 2kg harina + 3L leche + ... → 
  - Se descuenta de global
  - Se suma a branch: masa_crepa = 60
```

### 3. Venta: Combo 2 Dulces Separadas + Frappe Oreo
```
• Combo 2 Dulces Separadas:
  - masa_crepa: -2
  - charola: -1, tenedor: -2, servilleta: -2, papel_hamb: -1

• Frappe Oreo:
  - nutella_kg: -20g
  - oreo: -6 pz
  - vaso: -1, domo: -1, servilleta: -1
```

---

# Precios por Sucursal

## Atlixco

| Producto | Precio |
|---|---|
| Crepa Individual Dulce | $25 |
| Crepa Individual Salada | $35 |
| Combo 2 Dulces | $50 |
| Combo 2 Saladas | $70 |
| Combo Dulce+Salada | $65 |
| Boneless | $90 |
| Nuggets | $80 |
| Papas Fritas | $35 |
| Papas con Chorizo | $60 |
| Carlota | $40 |
| Tiramisú | $45 |
| Fresas/Duraznos con Crema | $50 |
| Frappe Oreo | $35 |
| Frappe Cocoa | $30 |
| Frappe Fresa | $30 |
| Frappe Fresa Cocoa | $30 |

## Metepec

| Producto | Precio |
|---|---|
| Crepa Individual Dulce | $25 |
| Crepa Individual Salada | $35 |
| Combo 2 Dulces | $40 |
| Combo 2 Saladas | $60 |
| Combo Dulce+Salada | $55 |
| Boneless | $90 |
| Nuggets | $70 |
| Papas Fritas | $35 |
| Papas con Chorizo | $60 |
| Carlota | $40 |
| Tiramisú | $45 |
| Fresas/Duraznos con Crema | $50 |
| Frappe Oreo | $35 |
| Frappe Cocoa | $30 |
| Frappe Fresa | $30 |
| Frappe Fresa Cocoa | $30 |

---

*Actualizado: Abril 2026*