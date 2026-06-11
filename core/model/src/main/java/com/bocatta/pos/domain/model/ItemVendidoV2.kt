package com.bocatta.pos.domain.model

/**
 * Representa una línea de venta dentro de una transacción completada.
 * Reemplaza el anti-patrón List<Any> en VentaV2 y SolicitudDevolucion.
 *
 * Diseñado para ser compatible con la serialización automática de Firestore
 * via data class (todos los campos tienen valor default).
 */
data class ItemVendidoV2(
    val cartId: String = "",
    val productoId: String = "",
    val nombre: String = "",
    val cantidad: Int = 1,
    val precioUnitario: Double = 0.0,
    val base: String? = null,
    val aderezos: List<String> = emptyList(),
    val toppings: List<String> = emptyList(),
    val separadas: Boolean = false,
    val recetaId: String? = null,
    /** Mapa de insumoId -> cantidad deducida en unidad base (g, pz, etc.) */
    val deducciones: Map<String, Double> = emptyMap(),
    val componentesCombo: List<ItemVendidoV2> = emptyList()
) {
    /** Subtotal calculado de esta línea: precio unitario * cantidad */
    val subtotal: Double get() = precioUnitario * cantidad
}

