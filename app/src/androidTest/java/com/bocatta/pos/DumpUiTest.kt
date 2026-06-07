package com.bocatta.pos

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DumpUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dumpUiTree() {
        composeTestRule.waitForIdle()
        Thread.sleep(2000)
        composeTestRule.onRoot().printToLog("MY_UI_DUMP")
    }
}
