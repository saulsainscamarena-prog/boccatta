package com.bocatta.pos.domain.model

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

data class ConsumibleRequerido(
    val consumibleId: String = "",
    val cantidad: Double = 1.0,
    val unidad: String = "pz"
)

/**
 * Receta de venta (deducción unitaria).
 * Guardada en Firestore [FirestoreCollections.RECETAS] (`v2_recetas`).
 * NO confundir con [RecipeV2] — esa es para producción batch y va en `v2_recetas_produccion`.
 */
data class RecetaV2(
    val id: String = "",
    val nombre: String = "",
    val productoId: String? = null,
    val ingredientes: List<IngredienteReceta> = emptyList(),
    val rendimientoPorcion: Double = 1.0
)

data class IngredienteReceta(
    val insumoId: String = "",
    val nombreInsumo: String = "",
    val cantidad: Double = 0.0,
    val unidad: String = "g"
)

data class ConsumibleV2(
    val id: String = "",
    val nombre: String = "",
    val unidadBase: String = "pz",
    val stockActual: Double = 0.0,
    val stockMinimo: Double = 50.0
)

data class ItemCarritoV2(
    val cartId: String = "",
    val producto: SalesInventoryProductV2,
    val precioFinal: BigDecimal,
    val cantidad: Int = 1,
    val nota: String = "",
    val nombre: String = "",
    val base: String? = null,
    val aderezos: List<String> = emptyList(),
    val toppings: List<String> = emptyList(),
    val esSeparado: Boolean = false,
    val componentesCombo: List<ItemCarritoV2> = emptyList()
) {
    fun copyConCantidad(nuevaCantidad: Int) = copy(cantidad = nuevaCantidad)
}

data class ClienteV2(
    val idDocumento: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val visitasCicloActual: Int = 0,
    val comprasCicloActual: List<Double> = emptyList(),
    val fechaUltimaVisita: Long = 0L
)

@Immutable
data class InsumoV2(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val unidadBase: String = "g",
    val costoUnitarioBase: Double = 0.0,
    val cantidadEnBase: Double = 0.0,
    val stockMinimo: Double = 10.0,
    val presentaciones: List<PresentacionInsumo> = emptyList(),
    val presentacionesCompra: List<PresentacionCompraPreview> = emptyList()
)

data class PresentacionInsumo(
    val nombre: String = "",
    val unidadEquivalente: String = "pz",
    val factorConversionABase: Double = 1.0,
    val cantidadDisponible: Double = 0.0,
    val ultimoPrecioPagado: Double = 0.0
)

data class PresentacionCompraPreview(
    val nombre: String = "",
    val descripcion: String = "",
    val contenidoSugerido: Double = 1.0,
    val contenidoMin: Double = 1.0,
    val contenidoMax: Double = 9999.0,
    val precioSugerido: Double = 0.0,
    val precioMin: Double = 0.0,
    val precioMax: Double = 99999.0
)

data class CompraRegistro(
    val id: String = "",
    val insumoId: String = "",
    val insumoNombre: String = "",
    val presentacion: String = "",
    val cantidadComprada: Double = 1.0,
    val contenidoUnidades: Double = 0.0,
    val precioPagado: Double = 0.0,
    val compradoPor: String = "",
    val compradoPorNombre: String = "",
    val fecha: Long = 0L,
    val sucursal: String = "",
    val auditada: Boolean = false,
    val estado: String = "pendiente",
    val motivoAjuste: String = "",
    val motivoPerdida: String = "",
    val resueltoPor: String = "",
    val resueltoPorNombre: String = "",
    val fechaResolucion: Long = 0L
)

data class VentaV2(
    val id: String = "",
    val ticket: Long = 0L,
    val numeroTicket: Long = 0L,
    val codigoTicket: String = "",
    val total: Double = 0.0,
    val descuentoLealtad: Double = 0.0,
    val fecha: Long = 0L,
    val sucursal: String = "",
    val atendio: String = "",
    val metodoPago: String = MetodoPago.EFECTIVO.valor,
    val esConsumoEmpleado: Boolean = false,
    val estado: String = "completada",
    val clienteId: String? = null,
    val productos: List<ItemVendidoV2> = emptyList(),
    val productosIds: List<String> = emptyList()
)

data class GastoV2(
    val id: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    val fecha: Long = 0L,
    val sucursal: String = "",
    val usuarioId: String = ""
)

data class RecetaTandaV2(
    val id: String = "",
    val nombre: String = "",
    val ingredientes: Map<String, Double> = emptyMap(),
    val cantidadResultante: Double = 0.0,
    val costoTandaEstimado: Double = 0.0
)

data class TurnoCajaV2(
    val id: String = "",
    val sucursal: String = "",
    val usuarioResponsable: String = "",
    val fechaApertura: Long = 0L,
    val fechaCierre: Long? = null,
    val fondoInicial: Double = 0.0,
    val totalVentasEfectivo: Double = 0.0,
    val totalVentasTarjeta: Double = 0.0,
    val totalGastosTurno: Double = 0.0,
    val efectivoContado: Double = 0.0,
    val tarjetaContada: Double = 0.0,
    val diferenciaEfectivo: Double = 0.0,
    val diferenciaTarjeta: Double = 0.0,
    val estado: String = "abierto"
)

enum class Rol { ADMIN, VENDEDOR, DUEÑO }

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: Rol = Rol.VENDEDOR
)

data class MermaV2(
    val id: String = "",
    val insumoId: String = "",
    val cantidad: Double = 0.0,
    val costo: Double = 0.0,
    val motivo: String = "",
    val comentario: String = "",
    val estado: String = "pendiente",
    val fecha: Long = 0L,
    val sucursal: String = "",
    val usuarioId: String = ""
)

data class EmpleadoV2(
    val id: String = "",
    val nombre: String = "",
    val rol: String = "VENDEDOR",
    val pinAcceso: String = "0000",
    val sucursalAsignada: String = ""
)

data class ProveedorV2(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val categoria: String = ""
)

data class VentaPorDia(
    val etiqueta: String = "",
    val total: Double = 0.0
)

data class ProductoMetrica(
    val nombre: String = "",
    val cantidad: Int = 0,
    val ingresos: Double = 0.0
)

data class SolicitudDevolucion(
    val id: String = "",
    val ventaId: String = "",
    val motivo: String = "",
    val solicitadoPor: String = "",
    val fecha: Long = 0L,
    val estado: String = "pendiente",
    val totalVenta: Double = 0.0,
    val productosVenta: List<ItemVendidoV2> = emptyList(),
    val productosIds: List<String> = emptyList(),
    val numeroTicket: Long = 0L
)

data class ItemConteo(
    val id: String = "",
    val nombre: String = "",
    val stockSistema: Double = 0.0,
    val conteoFisico: String = "",
    val costoUnitario: Double = 0.0
)

data class RegistroCompraV2(
    val id: String = "",
    val insumoId: String = "",
    val proveedorId: String = "",
    val cantidadComprada: Double = 0.0,
    val precioUnitarioCompra: Double = 0.0,
    val precioTotal: Double = 0.0,
    val fecha: Long = 0L,
    val sucursalRecibe: String = ""
)

enum class MetodoPago(val valor: String) {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia"),
    RAPPI("Rappi"),
    UBER_EATS("Uber Eats"),
    DIDI_FOOD("DiDi Food")
}

enum class TipoDescuentoPromo(val firestoreKey: String) { PORCENTAJE("porcentaje"), MONTO_FIJO_TICKET("monto_fijo_ticket"), MONTO_FIJO_ITEM("monto_fijo_item"), PRECIO_FIJO_ITEM("precio_fijo_item") }

enum class AlcancePromo(val firestoreKey: String) { TICKET_COMPLETO("ticket_completo"), CATEGORIAS_ESPECIFICAS("categorias_especificas"), PRODUCTOS_ESPECIFICOS("productos_especificos") }

data class PromocionUniversal(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val codigoCupon: String? = null,
    val tipoDescuento: TipoDescuentoPromo = TipoDescuentoPromo.PORCENTAJE,
    val valorDescuento: Double = 0.0,
    val descuentoMaximoTicket: Double? = null,
    val alcance: AlcancePromo = AlcancePromo.TICKET_COMPLETO,
    val itemsIncluidosIds: List<String> = emptyList(),
    val itemsExcluidosIds: List<String> = emptyList(),
    val montoMinimoTicket: Double = 0.0,
    val cantidadMinimaItems: Int = 1,
    val esBogo: Boolean = false,
    val bogoComprarCantidad: Int = 0,
    val bogoCobrarCantidad: Int = 0,
    val fechaInicio: Long = 0L,
    val fechaFin: Long? = null,
    val diasValidosSemana: List<Int> = emptyList(),
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val limiteUsosGlobal: Int? = null,
    val limiteUsosPorTicket: Int? = null,
    val usosActuales: Int = 0,
    val combinable: Boolean = false,
    val activa: Boolean = true,
    val etiquetas: List<String> = emptyList()
)

