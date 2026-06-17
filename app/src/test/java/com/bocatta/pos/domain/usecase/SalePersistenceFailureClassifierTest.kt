package com.bocatta.pos.domain.usecase

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalePersistenceFailureClassifierTest {

    @Test
    fun `io failure is transient`() {
        assertEquals(
            SalePersistenceFailureKind.TRANSIENT,
            SalePersistenceFailureClassifier.classify(IOException("timeout"))
        )
    }

    @Test
    fun `stock rejection is business failure`() {
        assertEquals(
            SalePersistenceFailureKind.BUSINESS,
            SalePersistenceFailureClassifier.classify(
                IllegalStateException("Stock insuficiente local")
            )
        )
    }

    @Test
    fun `permission denied is permanent and operator safe`() {
        val error = IllegalStateException("Firestore PERMISSION_DENIED while committing sale")

        val kind = SalePersistenceFailureClassifier.classify(error)

        assertEquals(SalePersistenceFailureKind.PERMANENT, kind)
        assertTrue(
            SalePersistenceFailureClassifier.operatorMessage(error, kind)
                .contains("permiso", ignoreCase = true)
        )
    }
}
