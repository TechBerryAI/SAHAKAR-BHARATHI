package com.example.sahakarbharati.ui.navigation

import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.DefaultAppLanguage
import com.example.sahakarbharati.core.copy.copyFor
import com.example.sahakarbharati.features.registration.formExtraCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageCoverageTest {

    @Test
    fun defaultLanguageIsEnglishAndDeviceTagsAreIgnored() {
        assertEquals(AppLanguage.EN, DefaultAppLanguage)
        assertEquals(AppLanguage.EN, AppLanguage.fromKey(""))
        assertEquals(AppLanguage.EN, AppLanguage.fromKey("hi-IN"))
        assertEquals(AppLanguage.EN, AppLanguage.fromKey("ta-IN"))
        val english = copyFor(DefaultAppLanguage)
        assertEquals("Register Now", english.getStarted)
        assertEquals("Choose language", english.chooseLanguage)
        assertEquals("Kisan Home", english.homeTitle)
        assertEquals("Contact Details", formExtraCopy(DefaultAppLanguage).contactTitle)
        assertFalse(DefaultAppLanguage.rightToLeft)
    }

    @Test
    fun selectingAnyLanguageRewritesTheWholeApp() {
        assertEquals(26, AppLanguage.entries.size)
        val english = copyFor(AppLanguage.EN)
        val englishForm = formExtraCopy(AppLanguage.EN)
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromKey(language.key))
            val copy = copyFor(language)
            val form = formExtraCopy(language)
            if (language == AppLanguage.EN) {
                return@forEach
            }
            assertNotEquals(language.key, english.homeTitle, copy.homeTitle)
            assertNotEquals(language.key, english.greeting, copy.greeting)
            assertNotEquals(language.key, english.tabHome, copy.tabHome)
            assertNotEquals(language.key, english.tabProfile, copy.tabProfile)
            assertNotEquals(language.key, english.profileTitle, copy.profileTitle)
            assertNotEquals(language.key, english.trackTitle, copy.trackTitle)
            assertNotEquals(language.key, english.attendanceTitle, copy.attendanceTitle)
            assertNotEquals(language.key, english.formTitle, copy.formTitle)
            assertNotEquals(language.key, english.continueNext, copy.continueNext)
            assertNotEquals(language.key, english.successTitle, copy.successTitle)
            assertNotEquals(language.key, english.getStarted, copy.getStarted)
            assertNotEquals(language.key, english.signIn, copy.signIn)
            assertNotEquals(language.key, english.chooseLanguage, copy.chooseLanguage)
            assertNotEquals(language.key, english.logOut, copy.logOut)
            assertNotEquals(language.key, english.logOutBody, copy.logOutBody)
            assertNotEquals(language.key, english.sosTitle, copy.sosTitle)
            assertNotEquals(language.key, english.close, copy.close)
            assertNotEquals(language.key, english.ok, copy.ok)
            assertNotEquals(language.key, english.cancel, copy.cancel)
            assertNotEquals(language.key, english.loginLabel, copy.loginLabel)
            assertNotEquals(language.key, english.noAccount, copy.noAccount)
            assertNotEquals(language.key, english.tileTrack, copy.tileTrack)
            assertNotEquals(language.key, english.tileQr, copy.tileQr)
            assertNotEquals(language.key, english.scanTab, copy.scanTab)
            assertNotEquals(language.key, english.farmer, copy.farmer)
            assertNotEquals(language.key, english.fisherman, copy.fisherman)
            assertNotEquals(language.key, english.yearsSuffix, copy.yearsSuffix)
            assertNotEquals(language.key, english.currentLocation, copy.currentLocation)
            assertNotEquals(language.key, english.createAccountTitle, copy.createAccountTitle)
            assertNotEquals(language.key, englishForm.contactTitle, form.contactTitle)
            assertNotEquals(language.key, englishForm.securityTitle, form.securityTitle)
            assertNotEquals(language.key, englishForm.photoTitle, form.photoTitle)
            assertNotEquals(language.key, englishForm.nameRequired, form.nameRequired)
            assertNotEquals(language.key, englishForm.pinMismatch, form.pinMismatch)
            assertNotEquals(language.key, englishForm.aadhaarLabel, form.aadhaarLabel)
            assertNotEquals(language.key, englishForm.useMyLocation, form.useMyLocation)
            assertTrue(copy.homeTitle.isNotBlank())
            assertEquals(language == AppLanguage.UR || language == AppLanguage.SD, language.rightToLeft)
        }
    }
}
