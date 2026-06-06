package com.bocatta.pos.domain.usecase

class HeldOrderCheckoutCompletionPolicy {

    fun heldOrderIdToDeleteOnSuccessfulCheckout(lastCompletedHeldOrderId: String?): String? {
        return lastCompletedHeldOrderId?.takeIf { it.isNotBlank() }
    }

    fun heldOrderIdToDeleteOnFailedCheckout(activeHeldOrderId: String?): String? {
        return null
    }
}
