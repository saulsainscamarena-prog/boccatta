package com.bocatta.pos.domain.model

enum class TipoCatalogo {
    ADEREZO, TOPPING, TOPPING_PREMIUM, BASE_UNTABLE,
    SABOR_FRAPPE, ESPOLVOREADO, PRESENTACION, EXTRAS
}

data class OpcionCatalogo(
    val id: String = "",
    val nombre: String = "",
    val tipo: TipoCatalogo = TipoCatalogo.TOPPING,
    val esPremium: Boolean = false,
    val activo: Boolean = true,
    val defecto: Boolean = false,
    val costoExtra: Double = 0.0,
    val creadoEn: Long = System.currentTimeMillis()
)
