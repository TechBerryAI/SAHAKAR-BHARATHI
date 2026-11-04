package com.example.sahakarbharati.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sahakarbharati.SahakarApp
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RathYatraScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // The app skips the splash for a stored session, so start every test signed out
    // instead of depending on whatever the device was last used for.
    @Before
    fun signOut() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SahakarApp
        app.container.session.clearSession()
        app.container.session.setLanguageKey("en")
    }

    @Test
    fun splash_registerNow_opensRegistration() {
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                RathYatraScreen()
            }
        }

        composeRule.onNodeWithText("Welcome to Sahakar Bharati").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Register Now").performClick()
        composeRule.onNodeWithText("Mobile number").assertIsDisplayed()
    }

    @Test
    fun home_showsProfileSummary_withoutRemovedFeatures() {
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                RathYatraScreen(initialStack = listOf(RathYatraDestinations.HOME))
            }
        }

        composeRule.onNodeWithText("Jai Kisan").assertIsDisplayed()
        composeRule.onNodeWithText("Registered participant").assertIsDisplayed()
        assertAbsent("Rath Route & National Map")
        assertAbsent("Rath Track")
        assertAbsent("Track Raths")
    }

    private fun assertAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }
}
