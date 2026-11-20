package com.example.sahakarbharati.features.yatra

import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.languagePickerOrder
import com.example.sahakarbharati.core.copy.uiPack
import com.example.sahakarbharati.core.copy.uiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YatraCopyTest {
    private val keys = listOf(
        "yatrasTitle",
        "yatrasCardBody",
        "noPublishedYatras",
        "noPublishedStops",
        "stopsHeading",
        "yatrasLoadFailed",
        "yatraLoadFailed",
        "yatraUnavailable",
        "signInRequired",
        "retry",
        "yatrasLoadMore",
        "yatrasLoadMoreFailed",
        "yatraCode",
        "arrival",
        "departure",
        "yatraDateRange",
        "yatraRowLabel",
        "statusDraft",
        "statusPlanned",
        "statusApprovalPending",
        "statusApproved",
        "statusUpcoming",
        "statusInProgress",
        "statusCompleted",
        "statusArchived",
    )

    @Test
    fun everySupportedLanguageHasTheYatraStrings() {
        assertEquals(26, languagePickerOrder.size)
        languagePickerOrder.forEach { language ->
            val pack = uiPack(language)
            keys.forEach { key ->
                val value = pack[key]
                assertTrue("$language is missing $key", !value.isNullOrBlank())
                assertNotEquals(key, value)
            }
        }
        assertNotEquals(
            uiText(AppLanguage.EN, "yatrasTitle"),
            uiText(AppLanguage.HI, "yatrasTitle"),
        )
    }

    @Test
    fun rowLabelKeepsEachFactInItsOwnSentence() {
        val label = uiText(
            AppLanguage.EN,
            "yatraRowLabel",
            mapOf(
                "name" to "Nashik Rath",
                "district" to "Nashik",
                "when" to "2026-07-01 – 2026-07-12",
                "status" to "Upcoming",
            ),
        )
        assertEquals("Nashik Rath. Nashik. 2026-07-01 – 2026-07-12. Upcoming.", label)
    }

    @Test
    fun dateDisplayStillUsesTheApiDatePrefix() {
        assertEquals("2026-07-01", yatraWhen("2026-07-01T09:30:00Z"))
        assertEquals("statusUpcoming", yatraStatusKey("upcoming"))
    }
}
