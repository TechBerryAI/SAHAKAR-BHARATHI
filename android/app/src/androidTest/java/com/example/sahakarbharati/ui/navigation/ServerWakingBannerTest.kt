package com.example.sahakarbharati.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme
import org.junit.Rule
import org.junit.Test

class ServerWakingBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bannerAppearsOnlyWhileTheServerIsSlow() {
        val slow = mutableStateOf(false)
        val message = "The server is starting up. This can take up to a minute, please wait…"
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                ServerWakingBanner(visible = slow.value, message = message)
            }
        }
        composeRule.onNodeWithText(message).assertDoesNotExist()
        slow.value = true
        composeRule.onNodeWithText(message).assertIsDisplayed()
        slow.value = false
        composeRule.waitForIdle()
        composeRule.onNodeWithText(message).assertDoesNotExist()
    }
}
