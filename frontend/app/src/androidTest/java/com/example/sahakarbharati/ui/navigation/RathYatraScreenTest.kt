package com.example.sahakarbharati.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RathYatraScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun splash_getStarted_opensWelcome() {
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                RathYatraScreen()
            }
        }

        composeRule.onNodeWithText("Sahakar Bharati").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Get Started").performClick()
        composeRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeRule.onNodeWithText("NATIONAL PORTAL").assertDoesNotExist()
    }

    @Test
    fun home_openTracking_andBackReturnsHome() {
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                RathYatraScreen(initialStack = listOf(RathYatraDestinations.HOME))
            }
        }

        composeRule.onNodeWithText("Kisan Home").assertIsDisplayed()
        composeRule.onNodeWithText("Live Rath Movement & National Route").performClick()
        composeRule.onNodeWithText("Rath Track").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        composeRule.onNodeWithText("Kisan Home").assertIsDisplayed()
    }
}
