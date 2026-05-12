package com.bocatta.pos.domain.model

data class DiscountV2(
    val id: String = "",
    val name: String = "",
    val type: String = "PERCENTAGE",
    val conditions: Map<String, Any> = emptyMap(),
    val value: Double = 0.0,
    val isActive: Boolean = true,
    val startDate: Long = 0L,
    val endDate: Long? = null
)
