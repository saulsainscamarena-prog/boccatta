package com.bocatta.pos.presentation.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bocatta.pos.feature.inventario.ui.screens.inventario.InventoryCardPremium
import com.bocatta.pos.feature.ventas.ui.screens.ventas.BocattaSalesNavigationRail
import com.bocatta.pos.presentation.ui.theme.BocattaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UxUiSystemInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tabletNavigation_exposesSingleCashDestinationAndAdminEntries() {
        composeTestRule.setContent {
            BocattaTheme(dynamicColor = false) {
                BocattaSalesNavigationRail(
                    esAdmin = true,
                    onVerCaja = {},
                    onVerInventario = {},
                    onVerGastos = {},
                    onVerDevoluciones = {},
                    onVerAdmin = {},
                    onVerReportes = {},
                    onVerActividad = {}
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Caja y cierre de turno")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithContentDescription("Control central")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithContentDescription("Reportes")
            .assertCountEquals(1)
    }

    @Test
    fun primaryButton_keepsCounterSizedTouchTarget() {
        composeTestRule.setContent {
            BocattaTheme(dynamicColor = false) {
                NeonButton(texto = "COBRAR", onClick = {})
            }
        }

        composeTestRule
            .onNodeWithText("COBRAR")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun inventoryCard_rendersInLightAndDarkThemes() {
        composeTestRule.setContent {
            Column {
                BocattaTheme(darkTheme = false, dynamicColor = false) {
                    InventoryCardPremium(
                        nombre = "Harina claro",
                        cantidad = 2.0,
                        unidad = "kg",
                        stockMinimo = 5.0
                    )
                }
                BocattaTheme(darkTheme = true, dynamicColor = false) {
                    InventoryCardPremium(
                        nombre = "Harina oscuro",
                        cantidad = 2.0,
                        unidad = "kg",
                        stockMinimo = 5.0
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Harina claro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Harina oscuro").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Bajo").assertCountEquals(2)
    }
}
