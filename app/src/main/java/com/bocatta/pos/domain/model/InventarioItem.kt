package com.bocatta.pos.domain.model

enum class TipoInsumo {
    PORCIONADO, // Se cuenta por unidad (Nuggets, Boneless, Papas)
    PREPARADO,  // Requiere receta (Masa Crepas, Tiramisu)
    GASTO       // Se registra pero no se descuenta rígidamente (Servilletas, Fresas)
}

data class PresentacionCompra(
    val nombre: String = "", // Ej: "Caja", "Paquete", "Garrafón"
    val factorConversion: Double = 1.0, // Ej: 294 (galletas por caja)
    val ultimoPrecioPagado: Double = 0.0
)

data class InventarioItem(
    val id: String = "",
    val nombre: String = "",
    val unidadBase: String = "", // "Pzas", "Gramos", "L"
    val stockActual: Double = 0.0,
    val stockCritico: Double = 10.0,
    val tipo: TipoInsumo = TipoInsumo.PORCIONADO,
    val presentaciones: List<PresentacionCompra> = emptyList()
)

