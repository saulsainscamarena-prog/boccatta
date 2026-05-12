package com.bocatta.pos.presentation.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponsiveLayoutInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bocattaSearchBar_rendersWithDefaultText() {
        composeTestRule.setContent {
            BocattaSearchBar(
                query = "",
                onQueryChange = {}
            )
        }

        composeTestRule.onNodeWithText("Buscar...").assertIsDisplayed()
    }

    @Test
    fun bocattaSearchBar_displaysQueryText() {
        composeTestRule.setContent {
            BocattaSearchBar(
                query = "crepa",
                onQueryChange = {}
            )
        }

        composeTestRule.onNodeWithText("crepa").assertIsDisplayed()
    }

    @Test
    fun bocattaSearchBar_clearButtonAppearsWhenNotEmpty() {
        composeTestRule.setContent {
            BocattaSearchBar(
                query = "test",
                onQueryChange = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Limpiar b\u00fasqueda").assertIsDisplayed()
    }

    @Test
    fun bocattaSearchBar_clearButtonHiddenWhenEmpty() {
        composeTestRule.setContent {
            BocattaSearchBar(
                query = "",
                onQueryChange = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Limpiar b\u00fasqueda").assertDoesNotExist()
    }

    @Test
    fun bocattaCartItemRow_displaysItemNameAndPrice() {
        composeTestRule.setContent {
            BocattaCartItemRow(
                item = com.bocatta.pos.domain.model.ItemCarritoV2(
                    producto = com.bocatta.pos.domain.model.SalesInventoryProductV2(
                        id = "p1", nombre = "Coca"
                    ),
                    precioFinal = java.math.BigDecimal("25.00"),
                    cantidad = 2,
                    nombre = "Coca Cola"
                ),
                onEliminar = {}
            )
        }

        composeTestRule.onNodeWithText("Coca Cola").assertIsDisplayed()
        composeTestRule.onNodeWithText("$50.00").assertIsDisplayed()
    }

    @Test
    fun bocattaCartItemRow_deleteButtonVisible() {
        composeTestRule.setContent {
            BocattaCartItemRow(
                item = com.bocatta.pos.domain.model.ItemCarritoV2(
                    producto = com.bocatta.pos.domain.model.SalesInventoryProductV2(
                        id = "p1", nombre = "Test"
                    ),
                    precioFinal = java.math.BigDecimal("10.00"),
                    nombre = "Test"
                ),
                onEliminar = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Eliminar Test").assertIsDisplayed()
    }

    @Test
    fun dynamicProductForm_durationField_incrementsCorrectly() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "SERVICE",
                initialValues = mapOf("durationMinutes" to 30),
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("30 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("35 min").assertIsDisplayed()
    }

    @Test
    fun dynamicProductForm_switchToggles() {
        composeTestRule.setContent {
            DynamicProductForm(
                giro = "FOOD",
                initialValues = mapOf("esCombo" to false),
                onValuesChanged = {}
            )
        }

        composeTestRule.onNodeWithText("Es Combo").performClick()
        composeTestRule.onNodeWithText("Es Combo").assertIsDisplayed()
    }

    @Test
    fun giroSelector_changingGiro_emitsCallback() {
        var selectedGiro = ""
        composeTestRule.setContent {
            GiroSelector(
                currentGiro = "FOOD",
                onGiroSelected = { selectedGiro = it }
            )
        }

        composeTestRule.onNodeWithText("Alimentos").performClick()
        composeTestRule.onNodeWithText("Servicios").performClick()
        assert(selectedGiro == "SERVICE")
    }

    @Test
    fun allGiroOptions_visibleInSelector() {
        composeTestRule.setContent {
            GiroSelector(
                currentGiro = "FOOD",
                onGiroSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Alimentos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Giro del Negocio").assertIsDisplayed()
    }
}
