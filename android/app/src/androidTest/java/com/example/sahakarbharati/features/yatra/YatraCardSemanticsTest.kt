package com.example.sahakarbharati.features.yatra

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sahakarbharati.core.copy.ui
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YatraCardSemanticsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clickableYatraCardIsOneNamedButton() {
        val label = "Nashik Rath. Nashik. 2026-07-01 – 2026-07-12. Upcoming."
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                WhiteCard(onClick = {}, semanticsLabel = label) {
                    Text("Nashik Rath")
                }
            }
        }
        composeRule.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Nashik Rath", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun loadMoreYatrasIsANamedButton() {
        composeRule.setContent {
            SahakarBharatiTheme(dynamicColor = false) {
                val label = ui("yatrasLoadMore")
                SaffronButton(text = label, onClick = {})
            }
        }
        composeRule.onNodeWithContentDescription("Load more Yatras").assertIsDisplayed().assertHasClickAction()
    }
}
