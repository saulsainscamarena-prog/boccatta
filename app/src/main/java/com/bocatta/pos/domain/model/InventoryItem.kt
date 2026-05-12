package com.bocatta.pos.domain.model

data class InventoryItem(
    val id: String = "",
    val branchId: String = "",
    val productId: String = "",
    val currentQty: Double = 0.0,
    val unit: String = "pza",
    val minThreshold: Double = 0.0,
    val lastUpdated: Long = 0L
) {
    fun isBelowThreshold(): Boolean = currentQty <= minThreshold
    fun hasStock(): Boolean = currentQty > 0.0
}
