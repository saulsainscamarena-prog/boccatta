package com.bocatta.pos.domain.model

data class MovementV2(
    val id: String = "",
    val branchId: String = "",
    val type: String = "ADJUSTMENT",
    val referenceId: String = "",
    val productId: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pza",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)
