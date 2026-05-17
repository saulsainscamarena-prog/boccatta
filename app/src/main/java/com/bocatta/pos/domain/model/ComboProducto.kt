package com.bocatta.pos.domain.model

enum class TipoCombo(val firestoreKey: String) { FIJO("fijo"), CONFIGURABLE("configurable") }

data class ComboProducto(
    val id: String = "",
    val nombre: String = "",
    val emoji: String = "🎁",
    val categoria: String = "",
    val subcategoria: String = "",
    val precioVenta: Map<String, Double> = emptyMap(),
    val tipo: TipoCombo = TipoCombo.FIJO,
    val productos: List<ItemCombo> = emptyList(),
    val configSchema: List<ConfigOptionGroup> = emptyList(),
    val activo: Boolean = true
)

data class ItemCombo(
    val productoId: String = "",
    val cantidad: Int = 1,
    val fijo: Boolean = true
)


