package com.bocatta.pos.domain.model

data class InventoryItem(
    val id: String = "",
    val branchId: String = "",
    val productId: String = "",
    val currentQty: Double = 0.0,
    val cantidadEnBase: Double = 0.0, // Campo compatible
    val unit: String = "pza",
    val minThreshold: Double = 0.0,
    val lastUpdated: Long = 0L
) {
    fun getActualQty(): Double = if (currentQty != 0.0) currentQty else cantidadEnBase
    fun isBelowThreshold(): Boolean = getActualQty() <= minThreshold
    fun hasStock(): Boolean = getActualQty() > 0.0
}

