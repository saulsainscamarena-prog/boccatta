# Design: Clientes/CRM — Lealtad y Ciclo de Visitas

## Arquitectura

### Data Model
Extender `ClienteV2` existente (ya tiene `visitasCicloActual`, `comprasCicloActual`, `fechaUltimaVisita`).

```kotlin
// En ModelsV2.kt — ya existe, solo verificar que tenga estos campos:
data class ClienteV2(
    val idDocumento: String = "",
    val tenantId: String = "",
    val businessType: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val visitasCicloActual: Int = 0,       // Total visits (for tier + cycle)
    val comprasCicloActual: List<Double> = emptyList(),  // Amounts in current cycle
    val fechaUltimaVisita: Long = 0L
)
```

No se crea MembresiaV2 separada — todo inline en ClienteV2 para simplicidad y velocidad de lectura.

### MembresiaManager (ya existe en domain/)
```kotlin
object MembresiaManager {
    const val UMBRAL_PLATINO = 50
    const val UMBRAL_ORO = 20
    
    data class EstadoMembresia(
        val esElegiblePremio: Boolean,
        val visitasRestantesParaPremio: Int,
        val nivel: String
    )
    
    fun verificarEstadoMembresia(visitasActuales: Int): EstadoMembresia
}
```

### Nuevo: MembresiaDiscountCalculator
```kotlin
object MembresiaDiscountCalculator {
    fun calcularPorcentajeDescuento(promedioCompras: Double): Double {
        return when {
            promedioCompras >= 500 -> 20.0
            promedioCompras >= 300 -> 15.0
            promedioCompras >= 100 -> 10.0
            else -> 5.0
        }
    }
    
    fun calcularPromedio(compras: List<Double>): Double {
        if (compras.isEmpty()) return 0.0
        return compras.average()
    }
}
```

### MembresiaRepository (nuevo en data/repository/)
```kotlin
class MembresiaRepository {
    suspend fun obtenerCliente(clienteId: String): ClienteV2?
    suspend fun incrementarVisita(clienteId: String, montoCompra: Double, premioAplicado: Boolean)
    suspend fun resetearCiclo(clienteId: String)
}
```

### Flujo de Checkout con Lealtad

```
1. Clerk selects ClienteV2 in SalesScreen
2. SalesViewModelV2 calls MembresiaManager.verificarEstadoMembresia(cliente.visitasCicloActual)
3. If esElegiblePremio:
   a. Calculate promedio = MembresiaDiscountCalculator.calcularPromedio(cliente.comprasCicloActual)
   b. Calculate discount % = MembresiaDiscountCalculator.calcularPorcentajeDescuento(promedio)
   c. Show notification in UI
   d. Apply discount to total (as descuentoLealtad)
4. On sale completion:
   a. MembresiaRepository.incrementarVisita(clienteId, total, premioAplicado)
   b. If premioAplicado: MembresiaRepository.resetearCiclo(clienteId)
```

### Firestore
- Clientes ya están en `CLIENTES = "v2_customers"`
- No se crea colección separada — los campos de membresía van inline en el documento del cliente
- Transacción atómica: incrementar visita + actualizar comprasCicloActual + reset si aplica

### UI Changes

#### ClientesScreen
- Badge de nivel (PLATA/ORO/PLATINO) en cada tarjeta de cliente
- Progreso de ciclo: "Visita X de 5"

#### SalesScreen (CarritoPanelV2 / DialogosVentas)
- Al seleccionar cliente, si es elegible: notificación "🎉 Beneficio disponible: X% OFF"
- Botón de aplicar/ignorar beneficio
- Línea de descuento por lealtad en el resumen del carrito

### Tests
- MembresiaManagerTest ya existe con 4 tests
- MembresiaDiscountCalculatorTest (nuevo)
- CustomerRepository + MembresiaRepository integration test
- Flujo checkout + membresía en SalesViewModelV2Test
