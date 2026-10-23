package com.example.sahakarbharati.ui.navigation

import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.copyFor
import com.example.sahakarbharati.core.copy.languagePickerOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageCoverageTest {

    @Test
    fun unknownOrDeviceTagsFallBackToEnglish() {
        assertEquals(AppLanguage.EN, AppLanguage.fromKey(""))
        assertEquals(AppLanguage.EN, AppLanguage.fromKey("hi-IN"))
        assertEquals(AppLanguage.EN, AppLanguage.fromKey("xx"))
        assertEquals("Get Started", copyFor(AppLanguage.EN).getStarted)
    }

    @Test
    fun pickerListsEverySupportedLanguageOnce() {
        assertEquals(26, AppLanguage.entries.size)
        assertEquals(AppLanguage.entries.toSet(), languagePickerOrder.toSet())
        assertEquals(languagePickerOrder.size, languagePickerOrder.toSet().size)
        assertEquals(AppLanguage.EN, languagePickerOrder.first())
    }

    @Test
    fun everyLanguageRoundTripsAndHasCopy() {
        val english = copyFor(AppLanguage.EN)
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromKey(language.key))
            val copy = copyFor(language)
            assertTrue(language.key, copy.getStarted.isNotBlank())
            assertTrue(language.key, copy.signIn.isNotBlank())
            assertTrue(language.key, copy.homeTitle.isNotBlank())
            assertTrue(language.key, copy.comingSoonBody.isNotBlank())
            if (language != AppLanguage.EN && language != AppLanguage.SAT) {
                // Santali's overlay is still English text; every other language is translated.
                assertNotEquals(language.key, english.getStarted, copy.getStarted)
            }
        }
    }
}
