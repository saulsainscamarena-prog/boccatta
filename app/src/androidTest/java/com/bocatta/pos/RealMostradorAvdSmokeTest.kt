package com.bocatta.pos

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RealMostradorAvdSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun realFirebase_loginTurnoAndSalesGate_doNotHitPermissionDenied() {
        val (email, pin) = realSmokeArgs()
        loginAndEnterSales(email, pin)
    }

    @Test
    fun realFirebase_simpleExactCashSale_clearsCart() {
        val (email, pin) = realSmokeArgs()
        loginAndEnterSales(email, pin)

        composeTestRule.waitUntilAtLeastOneExists(hasText("BONELESS"), timeoutMillis = 45_000)
        composeTestRule.onNodeWithText("BONELESS").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("$90.00"), timeoutMillis = 15_000)

        composeTestRule.onNode(hasText("COBRAR") and hasClickAction()).performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Pago final"), timeoutMillis = 15_000)
        composeTestRule.onNodeWithText("Cobrar efectivo exacto").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("Cambio"), timeoutMillis = 10_000)
        composeTestRule.onNode(hasText("Confirmar pago") and hasClickAction()).performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasText("ORDEN VACIA"), timeoutMillis = 45_000)
        assertNoPermissionDeniedVisible()
    }

    private fun realSmokeArgs(): Pair<String, String> {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("bocatta.email").orEmpty()
        val pin = args.getString("bocatta.pin").orEmpty()
        assumeTrue("Real AVD smoke test skipped: missing bocatta.email/bocatta.pin args.", email.isNotBlank() && pin.isNotBlank())
        return email to pin
    }

    private fun loginAndEnterSales(email: String, pin: String) {
        composeTestRule.waitForIdle()

        if (composeTestRule.onAllNodesWithText("ENTRAR AL SISTEMA").fetchSemanticsNodes().isNotEmpty()) {
            val textFields = composeTestRule.onAllNodes(hasSetTextAction())
            textFields[0].performTextClearance()
            textFields[0].performTextInput(email)
            textFields[1].performTextClearance()
            textFields[1].performTextInput(pin)
            composeTestRule.onNodeWithText("ENTRAR AL SISTEMA").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 35_000) {
            composeTestRule.onAllNodesWithText("TURNOS").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("ORDEN VACIA").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeTestRule.onAllNodesWithText("ORDEN VACIA").fetchSemanticsNodes().isNotEmpty()) {
            assertNoPermissionDeniedVisible()
            return
        }
        assertNoPermissionDeniedVisible()
        assertNoProfileRequiredVisible()

        when {
            composeTestRule.onAllNodesWithText("UNIRSE AL TURNO").fetchSemanticsNodes().isNotEmpty() -> {
                composeTestRule.onNodeWithText("UNIRSE AL TURNO").performClick()
                composeTestRule.waitUntilAtLeastOneExists(hasText("ORDEN VACIA"), timeoutMillis = 35_000)
                assertNoPermissionDeniedVisible()
                return
            }
            composeTestRule.onAllNodesWithText("INICIAR TURNO").fetchSemanticsNodes().isNotEmpty() -> {
                abrirTurnoHastaMostrador()
            }
        }
    }

    private fun abrirTurnoHastaMostrador() {
        composeTestRule.onNodeWithText("INICIAR TURNO").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("INICIO DE JORNADA"), timeoutMillis = 25_000)
        assertNoPermissionDeniedVisible()

        if (composeTestRule.onAllNodesWithText("CONTINUAR").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("CONTINUAR").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 45_000) {
            composeTestRule.onAllNodesWithText("CONFIRMAR ASIGNACION").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("FONDO DE CAJA").fetchSemanticsNodes().isNotEmpty()
        }
        assertNoPermissionDeniedVisible()

        if (composeTestRule.onAllNodesWithText("CONFIRMAR ASIGNACION").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("CONFIRMAR ASIGNACION").performClick()
            composeTestRule.waitUntilAtLeastOneExists(hasText("FONDO DE CAJA"), timeoutMillis = 45_000)
        }

        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        textFields[0].performTextClearance()
        textFields[0].performTextInput("100")
        composeTestRule.onNodeWithText("COMENZAR JORNADA").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("ORDEN VACIA"), timeoutMillis = 45_000)
        assertNoPermissionDeniedVisible()
    }

    private fun assertNoPermissionDeniedVisible() {
        composeTestRule.onAllNodes(hasText("PERMISSION_DENIED", substring = true))
            .fetchSemanticsNodes()
            .let { nodes ->
                check(nodes.isEmpty()) { "PERMISSION_DENIED visible in real AVD smoke flow." }
            }
    }

    private fun assertNoProfileRequiredVisible() {
        composeTestRule.onAllNodes(hasText("PERFIL REQUERIDO", substring = true))
            .fetchSemanticsNodes()
            .let { nodes ->
                check(nodes.isEmpty()) { "Operational profile was not loaded after login." }
            }
    }
}
