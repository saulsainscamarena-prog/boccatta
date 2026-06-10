# Spec: Clientes/CRM — Lealtad y Ciclo de Visitas

## Requisitos Funcionales

### RF1: MembresiaManager — Estado de membresía
- `MembresiaManager.verificarEstadoMembresia(visitasTotales)` ya existe y funciona
- Cada `MEMBRESIA_CICLO_VISITAS` (5) visitas totales → `esElegiblePremio = true`
- Tiers: PLATA (0-19), ORO (20-49), PLATINO (50+)

### RF2: Cálculo de descuento por promedio de compras
- Al ser elegible para premio, calcular promedio de `comprasCicloActual`
- Mapear promedio a % de descuento:

| Promedio compras | % descuento |
|---|---|
| < $100 | 5% |
| $100 - $299 | 10% |
| $300 - $499 | 15% |
| $500+ | 20% |

### RF3: Flujo de checkout con lealtad
- Al seleccionar cliente en venta, verificar si es elegible para premio
- Si es elegible, mostrar notificación y aplicar descuento automático al total
- El descuento se muestra como línea separada en el ticket
- Después de completar la venta: incrementar `visitasCicloActual`, agregar total a `comprasCicloActual`, resetear `comprasCicloActual` si se aplicó premio

### RF4: UI de membresía
- Badge de nivel (PLATA/ORO/PLATINO) en tarjeta de cliente
- Indicador de progreso: "Visita X de 5 para tu próximo beneficio"
- En checkout: notificación cuando el cliente es elegible para premio
- Historial de visitas y premios canjeados

### RF5: MembresiaRepository
- `obtenerMembresia(clienteId): ClienteV2?`
- `incrementarVisita(clienteId, montoCompra): actualiza visitasCicloActual, comprasCicloActual, fechaUltimaVisita`
- `resetearCiclo(clienteId): limpia comprasCicloActual`

## Escenarios

### Escenario 1: Cliente nuevo, sin membresía
1. Cliente sin registro se presenta
2. Cajero registra cliente nuevo con nombre y teléfono
3. Se crea ClienteV2 con valores por defecto (visitas=0, compras=[], nivel=PLATA)
4. Al cobrar, no hay badge de lealtad ni descuento

### Escenario 2: Quinta visita — premio automático
1. Cliente con 4 visitas previas ($80, $120, $90, $110) se identifica
2. Sistema detecta: visita 5 → elegible para premio
3. Calcula promedio = $100 → 10% descuento
4. Muestra notificación "🎉 5ta visita! 10% OFF aplicado"
5. Total se reduce 10%
6. Al completar: visitas=5, comprasCicloActual se resetea para nuevo ciclo

### Escenario 3: Tier ORO
1. Cliente con 22 visitas totales se identifica
2. Badge muestra "ORO" en la tarjeta
3. Próximo premio en 3 visitas (25 % 5 = 0 → visita 25)

### Escenario 4: Sin premio disponible
1. Cliente con 3 visitas se identifica
2. Badge muestra nivel, progreso "Visita 3 de 5"
3. No hay descuento automático
4. Venta procede normalmente

## No funcionales
- Las operaciones de membresía deben ser transaccionales (Firestore transaction)
- No debe bloquear el flujo de checkout si Firestore falla
- Strings de UI en español (Membresía, Visitas, Nivel, Beneficio)
