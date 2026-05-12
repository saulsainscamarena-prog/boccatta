package com.bocatta.pos.presentation.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BocattaComponentsInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bocattaButton_displaysText() {
        composeTestRule.setContent {
            BocattaButton(
                texto = "Cobrar",
                onClick = {}
            )
        }

        composeTestRule.onNodeWithText("Cobrar").assertIsDisplayed()
    }

    @Test
    fun bocattaButton_firesOnClick() {
        var clicked = false
        composeTestRule.setContent {
            BocattaButton(
                texto = "Cobrar",
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Cobrar").performClick()
        assert(clicked)
    }

    @Test
    fun bocattaButton_disabled_doesNotFire() {
        var clicked = false
        composeTestRule.setContent {
            BocattaButton(
                texto = "Cobrar",
                onClick = { clicked = true },
                enabled = false
            )
        }

        composeTestRule.onNodeWithText("Cobrar").performClick()
        assert(!clicked)
    }

    @Test
    fun bocattaBadge_displaysText() {
        composeTestRule.setContent {
            BocattaBadge(
                texto = "ACTIVO",
                color = Color.Green
            )
        }

        composeTestRule.onNodeWithText("ACTIVO").assertIsDisplayed()
    }

    @Test
    fun bocattaMetricCard_displaysTitleAndValue() {
        composeTestRule.setContent {
            BocattaMetricCard(
                titulo = "Ventas Hoy",
                valor = "$1,250",
                color = Color.Black
            )
        }

        composeTestRule.onNodeWithText("VENTAS HOY").assertIsDisplayed()
        composeTestRule.onNodeWithText("$1,250").assertIsDisplayed()
    }

    @Test
    fun bocattaTopBar_displaysTitle() {
        composeTestRule.setContent {
            BocattaTopBar(
                title = "Menú Principal"
            )
        }

        composeTestRule.onNodeWithText("Menú Principal").assertIsDisplayed()
    }

    @Test
    fun bocattaTopBar_displaysSubtitle() {
        composeTestRule.setContent {
            BocattaTopBar(
                title = "Menú",
                subtitle = "Atlixco"
            )
        }

        composeTestRule.onNodeWithText("Atlixco").assertIsDisplayed()
    }

    @Test
    fun normalizarCategoria_handlesCommonCases() {
        assert(normalizarCategoria("crepas_dulces") == "Crepas Dulces")
        assert(normalizarCategoria("snacks") == "Snacks")
        assert(normalizarCategoria("bebidas") == "Bebidas")
        assert(normalizarCategoria("servicio") == "Servicios")
        assert(normalizarCategoria("unknown") == "Unknown")
    }

    @Test
    fun bocattaSectionTitle_displaysText() {
        composeTestRule.setContent {
            BocattaSectionTitle(
                texto = "Productos Populares"
            )
        }

        composeTestRule.onNodeWithText("Productos Populares").assertIsDisplayed()
    }

    @Test
    fun bocattaEmptyState_displaysIconAndTitle() {
        composeTestRule.setContent {
            BocattaEmptyState(
                icono = androidx.compose.material.icons.Icons.Default.SearchOff,
                titulo = "Sin resultados",
                descripcion = "Intenta con otro término"
            )
        }

        composeTestRule.onNodeWithText("Sin resultados").assertIsDisplayed()
        composeTestRule.onNodeWithText("Intenta con otro término").assertIsDisplayed()
    }

    @Test
    fun bocattaFilaResumen_displaysLabelAndValue() {
        composeTestRule.setContent {
            BocattaFilaResumen(
                etiqueta = "Subtotal",
                valor = "$100.00"
            )
        }

        composeTestRule.onNodeWithText("Subtotal").assertIsDisplayed()
        composeTestRule.onNodeWithText("$100.00").assertIsDisplayed()
    }
}
