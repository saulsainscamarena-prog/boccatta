package com.bocatta.pos.domain.model

data class SalesInventoryProductV2(
    val id: String = "",
    val nombre: String = "",
    val emoji: String = "🍩",
    val categoria: String = "",
    val precioVenta: Map<String, Double> = emptyMap(),
    val esCombo: Boolean = false,
    val productosCombo: List<String> = emptyList(),
    val recetaId: String? = null,
    val toppingsIncluidos: Int = 2,
    val costoToppingExtra: Double = 10.0,
    val esProductoTopping: Boolean = false,
    val consumiblesAsociados: List<ConsumibleRequerido> = emptyList(),
    val fotoUrl: String? = null,
    val configSchema: List<ConfigOptionGroup> = emptyList(),
    val subcategoria: String = "",
    val tipoProducto: String = "PREPARADO",
    val comboMode: String = "COMBO_ONLY",
    val rendimientoTanda: Int = 1,
    val unidadCompra: String = "",
    val pesoPorcion: Double? = null,
    val overrideGrupos: OverrideGrupos = OverrideGrupos(),
    val activo: Boolean = true
)

