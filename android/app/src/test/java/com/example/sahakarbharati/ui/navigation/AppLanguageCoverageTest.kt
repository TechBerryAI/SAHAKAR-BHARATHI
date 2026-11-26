package com.example.sahakarbharati.ui.navigation

import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.copyFor
import com.example.sahakarbharati.core.copy.languagePickerOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageCoverageTest {

    @Test
    fun unknownOrDeviceTagsFallBackToEnglish() {
        assertEquals(AppLanguage.EN, AppLanguage.fromKey(""))
        assertEquals(AppLanguage.EN, AppLanguage.fromKey("hi-IN"))
        assertEquals(AppLanguage.EN, AppLanguage.fromKey("xx"))
        assertEquals("Register Now", copyFor(AppLanguage.EN).getStarted)
    }

    @Test
    fun pickerListsEverySupportedLanguageOnce() {
        assertEquals(26, AppLanguage.entries.size)
        assertEquals(AppLanguage.entries.toSet(), languagePickerOrder.toSet())
        assertEquals(languagePickerOrder.size, languagePickerOrder.toSet().size)
        assertEquals(AppLanguage.EN, languagePickerOrder.first())
    }
}
