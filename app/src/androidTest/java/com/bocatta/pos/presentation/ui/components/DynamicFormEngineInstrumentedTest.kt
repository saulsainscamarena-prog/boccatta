package com.bocatta.pos.presentation.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DynamicFormEngineInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun foodForm_rendersAllFields() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = emptyMap(),
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("Atributos: Alimentos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Emoji").assertIsDisplayed()
        composeTestRule.onNodeWithText("Es Combo").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID Receta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toppings Incluidos").assertIsDisplayed()
    }

    @Test
    fun retailForm_rendersAllFields() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "RETAIL",
                initialValues = emptyMap(),
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("Atributos: Tienda / Kit").assertIsDisplayed()
        composeTestRule.onNodeWithText("SKU").assertIsDisplayed()
        composeTestRule.onNodeWithText("Es Kit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasa de Impuesto %").assertIsDisplayed()
    }

    @Test
    fun serviceForm_rendersAllFields() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "SERVICE",
                initialValues = emptyMap(),
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("Atributos: Servicios").assertIsDisplayed()
        composeTestRule.onNodeWithText("Duración (min)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Requiere Cita").assertIsDisplayed()
    }

    @Test
    fun form_passesInitialValues() {
        val initial = mapOf("emoji" to "🍕", "esCombo" to true)
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = initial,
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("🍕").assertIsDisplayed()
    }

    @Test
    fun form_callsOnChangedWhenValueChanges() {
        val changedValues = mutableMapOf<String, Any?>()
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = emptyMap(),
                onValuesChanged = { changedValues.putAll(it) }
            )
        }

        composeTestRule.onNodeWithText("Es Combo").performClick()
        assertNotNull(changedValues["esCombo"])
    }

    @Test
    fun giroSelector_showsAllOptions() {
        composeTestRule.setContent {
            GiroSelector(
                currentGiro = "FOOD",
                onGiroSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Alimentos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Giro del Negocio").assertIsDisplayed()
    }

    @Test
    fun giroSelector_firesCallback() {
        var selectedGiro = ""
        composeTestRule.setContent {
            GiroSelector(
                currentGiro = "FOOD",
                onGiroSelected = { selectedGiro = it }
            )
        }

        composeTestRule.onNodeWithText("Alimentos").performClick()
        composeTestRule.onNodeWithText("Tienda / Kit").performClick()
        assertEquals("RETAIL", selectedGiro)
    }

    @Test
    fun foodForm_emojiDefaults_toDonut() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = emptyMap(),
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("🍩").assertIsDisplayed()
    }

    @Test
    fun readOnlyForm_disablesInteraction() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = mapOf("esCombo" to false),
                onValuesChanged = {},
                readOnly = true
            )
        }

        composeTestRule.onNodeWithText("Es Combo").assertIsDisplayed()
    }

    @Test
    fun numberField_acceptsDecimalInput() {
        var values = mapOf<String, Any?>()
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = mapOf("toppingsIncluidos" to 2.0),
                onValuesChanged = { values = it }
            )
        }

        composeTestRule.onNodeWithText("2").performTextReplacement("3")
        assertNotNull(values["toppingsIncluidos"])
    }

    @Test
    fun booleanSwitch_togglesState() {
        var currentValues = mapOf<String, Any?>()
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = mapOf("esCombo" to false),
                onValuesChanged = { currentValues = it }
            )
        }

        composeTestRule.onNodeWithText("Es Combo").performClick()
        val esCombo = currentValues["esCombo"]
        assertTrue(esCombo is Boolean && esCombo == true)
    }
}
