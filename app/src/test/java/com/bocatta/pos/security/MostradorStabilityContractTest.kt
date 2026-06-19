package com.bocatta.pos.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MostradorStabilityContractTest {

    private val root: File
        get() = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { File(it, "app").exists() }

    @Test
    fun salesViewModelUsesMonotonicCartIdsInsteadOfTimestampOnlyIds() {
        val source = File(
            root,
            "feature/ventas/src/main/java/com/bocatta/pos/feature/ventas/viewmodel/SalesViewModelV2.kt"
        ).readText()

        assertTrue(source.contains("private val cartIdSequence = AtomicLong"))
        assertTrue(source.contains("fun generarCartId"))
        assertFalse(source.contains("cart_\${System.currentTimeMillis()}"))
    }

    @Test
    fun manualDiscountIsStoredAsPercentageAndDerivedFromCurrentCartTotal() {
        val source = File(
            root,
            "feature/ventas/src/main/java/com/bocatta/pos/feature/ventas/viewmodel/SalesViewModelV2.kt"
        ).readText()

        assertTrue(source.contains("_descuentoManualPorcentaje"))
        assertTrue(source.contains("val descuentoManual by derivedStateOf"))
        assertFalse(source.contains("private var _descuentoManual by mutableStateOf"))
    }
}
