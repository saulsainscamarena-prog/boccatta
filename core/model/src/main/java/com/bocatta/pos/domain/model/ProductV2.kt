package com.bocatta.pos.domain.model

data class InventoryProductV2(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val category: String = "",
    val type: String = "FINISHED",
    val basePrice: Double = 0.0,
    val status: String = "ACTIVE",
    val giro: String = "FOOD",
    val baseUnit: String = "pza",
    val trackInventory: Boolean = true,
    val attributes: Map<String, Any> = emptyMap(),
    val imageUrl: String? = null
) {
    fun isActive(): Boolean = status == "ACTIVE"
    fun isRaw(): Boolean = type == "RAW"
    fun isFinished(): Boolean = type == "FINISHED"
    fun isService(): Boolean = type == "SERVICE"
    fun isKit(): Boolean = type == "KIT"
}

