package com.bocatta.pos.domain.model

enum class TipoCatalogo(val firestoreKey: String) {
    ADEREZO("aderezo"), TOPPING("topping"), TOPPING_PREMIUM("topping_premium"), BASE_UNTABLE("base_untable"),
    SABOR_FRAPPE("sabor_frappe"), ESPOLVOREADO("espolvoreado"), PRESENTACION("presentacion"), EXTRAS("extras")
}

data class OpcionCatalogo(
    val id: String = "",
    val nombre: String = "",
    val tipo: TipoCatalogo = TipoCatalogo.TOPPING,
    val esPremium: Boolean = false,
    val activo: Boolean = true,
    val defecto: Boolean = false,
    val costoExtra: Double = 0.0,
    val creadoEn: Long = 0L
)

