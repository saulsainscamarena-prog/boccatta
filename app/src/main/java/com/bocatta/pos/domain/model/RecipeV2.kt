package com.bocatta.pos.domain.model

data class RecipeV2(
    val id: String = "",
    val productId: String = "",
    val yield: Double = 1.0,
    val yieldUnit: String = "pza",
    val inputs: List<RecipeInput> = emptyList(),
    val status: String = "ACTIVE"
)

data class RecipeInput(
    val insumoId: String = "",
    val qty: Double = 0.0,
    val unit: String = "g"
)
