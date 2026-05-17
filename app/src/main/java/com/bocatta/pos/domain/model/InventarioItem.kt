package com.bocatta.pos.domain.model

enum class TipoInsumo(val firestoreKey: String) {
    PORCIONADO("porcionado"),
    PREPARADO("preparado"),
    GASTO("gasto")
}

data class PresentacionCompra(
    val nombre: String = "", // Ej: "Caja", "Paquete", "Garrafón"
    val factorConversion: Double = 1.0, // Ej: 294 (galletas por caja)
    val ultimoPrecioPagado: Double = 0.0,
    val contenido: Int = 1,
    val unidadBase: String = "pza",
    val subunidades: Int = 1
) {
    fun totalUnidades(cantidadComprada: Int): Int = cantidadComprada * contenido * subunidades
}

data class InventarioItem(
    val id: String = "",
    val nombre: String = "",
    val unidadBase: String = "", // "Pzas", "Gramos", "L"
    val stockActual: Double = 0.0,
    val stockCritico: Double = 10.0,
    val tipo: TipoInsumo = TipoInsumo.PORCIONADO,
    val presentaciones: List<PresentacionCompra> = emptyList()
)

