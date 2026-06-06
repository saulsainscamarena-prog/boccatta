package com.bocatta.pos.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeldOrderCheckoutCompletionPolicyTest {
    private val policy = HeldOrderCheckoutCompletionPolicy()

    @Test
    fun successfulCheckoutWithHeldOrder_returnsOrderIdToDelete() {
        assertEquals(
            "held_123",
            policy.heldOrderIdToDeleteOnSuccessfulCheckout("held_123")
        )
    }

    @Test
    fun successfulCheckoutWithoutHeldOrder_doesNotDeleteAnything() {
        assertNull(policy.heldOrderIdToDeleteOnSuccessfulCheckout(null))
        assertNull(policy.heldOrderIdToDeleteOnSuccessfulCheckout(""))
    }

    @Test
    fun failedCheckout_keepsActiveHeldOrder() {
        assertNull(policy.heldOrderIdToDeleteOnFailedCheckout("held_123"))
        assertNull(policy.heldOrderIdToDeleteOnFailedCheckout(null))
    }
}
