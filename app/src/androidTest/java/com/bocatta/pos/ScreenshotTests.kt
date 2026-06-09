package com.bocatta.pos

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bocatta.pos.MainActivity
import com.dropbox.dropshots.Dropshots
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val dropshots = Dropshots()

    @Test
    fun testLoginScreen_Screenshot() {
        // Wait for login screen to load
        // Thread.sleep(2000)
        
        dropshots.assertSnapshot(
            view = composeTestRule.activity.window.decorView,
            name = "LoginScreen"
        )
    }
}
