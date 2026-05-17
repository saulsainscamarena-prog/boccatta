package com.bocatta.pos.domain.model

data class TransactionV2(
    val id: String = "",
    val branchId: String = "",
    val userId: String = "",
    val type: String = "SALE",
    val status: String = "COMPLETED",
    val subtotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMethods: List<String> = listOf("EFECTIVO"),
    val items: List<TransactionItemV2> = emptyList(),
    val createdAt: Long = 0L
)

data class TransactionItemV2(
    val productId: String = "",
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "pza",
    val unitPrice: Double = 0.0,
    val discountApplied: Double = 0.0,
    val taxRate: Double = 0.0,
    val subtotal: Double = 0.0,
    val modifiers: List<TransactionModifier> = emptyList()
)

data class TransactionModifier(
    val name: String = "",
    val price: Double = 0.0,
    val insumoId: String? = null,
    val qty: Double = 0.0,
    val unit: String = "pza"
)
