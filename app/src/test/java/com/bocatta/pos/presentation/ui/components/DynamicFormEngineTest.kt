package com.bocatta.pos.presentation.ui.components

import com.bocatta.pos.domain.model.InventoryProductV2
import org.junit.Assert.*
import org.junit.Test

class DynamicFormEngineTest {

    @Test
    fun getSchema_food_returnsFoodSchema() {
        val schema = DynamicFormEngine.getSchema("FOOD")
        assertEquals("FOOD", schema.giro)
        assertEquals("Alimentos", schema.label)
        assertEquals(6, schema.fields.size)
    }

    @Test
    fun getSchema_retail_returnsRetailSchema() {
        val schema = DynamicFormEngine.getSchema("RETAIL")
        assertEquals("RETAIL", schema.giro)
        assertEquals("Tienda / Kit", schema.label)
        assertEquals(5, schema.fields.size)
    }

    @Test
    fun getSchema_service_returnsServiceSchema() {
        val schema = DynamicFormEngine.getSchema("SERVICE")
        assertEquals("SERVICE", schema.giro)
        assertEquals(5, schema.fields.size)
    }

    @Test
    fun getSchema_unknown_returnsFoodAsDefault() {
        val schema = DynamicFormEngine.getSchema("UNKNOWN")
        assertEquals("FOOD", schema.giro)
    }

    @Test
    fun getSupportedGiros_returnsAll() {
        val giros = DynamicFormEngine.getSupportedGiros()
        assertEquals(3, giros.size)
        assertTrue(giros.any { it.giro == "FOOD" })
        assertTrue(giros.any { it.giro == "RETAIL" })
        assertTrue(giros.any { it.giro == "SERVICE" })
    }

    @Test
    fun extractValues_usesProductGiro() {
        val product = InventoryProductV2(id = "p1", name = "Test", giro = "SERVICE",
            attributes = mapOf("durationMinutes" to 45))
        val values = DynamicFormEngine.extractValues(product)
        val duration = (values["durationMinutes"] as? Number)?.toDouble() ?: 0.0
        assertEquals(45.0, duration, 0.01)
    }

    @Test
    fun extractValues_fallsBackToDefaults() {
        val product = InventoryProductV2(id = "p1", name = "Test", giro = "FOOD")
        val values = DynamicFormEngine.extractValues(product)
        assertEquals("🍩", values["emoji"] as String)
        assertEquals(false, values["esCombo"])
    }

    @Test
    fun buildAttributes_roundTrip() {
        val schema = DynamicFormEngine.getSchema("FOOD")
        val input = mapOf<String, Any?>("emoji" to "🍕", "esCombo" to true)
        val attrs = DynamicFormEngine.buildAttributes(schema, input)
        assertEquals("🍕", attrs["emoji"])
        assertEquals(true, attrs["esCombo"])
    }

    @Test
    fun caseInsensitiveGiro() {
        val lower = DynamicFormEngine.getSchema("food")
        val upper = DynamicFormEngine.getSchema("FOOD")
        assertEquals(lower.giro, upper.giro)
    }

    @Test
    fun allFieldTypesAreRepresented() {
        val hasTextField = DynamicFormEngine.getSchema("FOOD").fields.any { it is FormField.TextField }
        val hasNumberField = DynamicFormEngine.getSchema("FOOD").fields.any { it is FormField.NumberField }
        val hasBoolField = DynamicFormEngine.getSchema("FOOD").fields.any { it is FormField.BooleanField }
        assertTrue(hasTextField)
        assertTrue(hasNumberField)
        assertTrue(hasBoolField)
    }

    @Test
    fun retailHasSelectField() {
        val hasSelect = DynamicFormEngine.getSchema("RETAIL").fields.any { it is FormField.SelectField }
        assertTrue(hasSelect)
    }
}
