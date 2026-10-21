package com.example.sahakarbharati.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.copyFor

class RathYatraNavigationTest {

    @Test
    fun destinations_areUniqueAndIncludeEightStitchScreens() {
        val ids = RathYatraDestinations.all
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.contains(RathYatraDestinations.SPLASH))
        assertTrue(ids.contains(RathYatraDestinations.WELCOME))
        assertTrue(ids.contains(RathYatraDestinations.REGISTRATION_INTRO))
        assertTrue(ids.contains(RathYatraDestinations.REGISTRATION_FORM))
        assertTrue(ids.contains(RathYatraDestinations.REGISTRATION_SUCCESS))
        assertTrue(ids.contains(RathYatraDestinations.HOME))
        assertTrue(ids.contains(RathYatraDestinations.TRACKING))
        assertTrue(ids.contains(RathYatraDestinations.QR_ATTENDANCE))
    }

    @Test
    fun startDestination_isSplash() {
        assertEquals(RathYatraDestinations.SPLASH, RathYatraDestinations.startDestination)
    }

    @Test
    fun navigateStack_pushesAndPopsOnboardingFlow() {
        var stack = listOf(RathYatraDestinations.SPLASH)
        stack = navigateStack(stack, RathYatraDestinations.WELCOME)
        stack = navigateStack(stack, RathYatraDestinations.REGISTRATION_FORM)
        assertEquals(
            listOf(
                RathYatraDestinations.SPLASH,
                RathYatraDestinations.WELCOME,
                RathYatraDestinations.REGISTRATION_FORM,
            ),
            stack,
        )
        stack = popStack(stack)
        assertEquals(RathYatraDestinations.WELCOME, stack.last())
    }

    @Test
    fun navigateStack_homeReplacesRoot() {
        val stack = navigateStack(
            listOf(
                RathYatraDestinations.SPLASH,
                RathYatraDestinations.REGISTRATION_SUCCESS,
            ),
            RathYatraDestinations.HOME,
            replaceRoot = true,
        )
        assertEquals(listOf(RathYatraDestinations.HOME), stack)
    }

    @Test
    fun unknownDestination_recoversToHome() {
        val stack = navigateStack(listOf(RathYatraDestinations.HOME), "does_not_exist")
        assertEquals(listOf(RathYatraDestinations.HOME), stack)
    }

    @Test
    fun englishAndHindiCopy_honourFarmersNationwide() {
        val english = copyFor(AppLanguage.EN)
        val hindi = copyFor(AppLanguage.HI)
        assertEquals("Sahakar Bharati", english.title)
        assertEquals("Farmers & Fishermen Celebration", english.subtitle)
        assertFalse(english.portalChip.contains("demo", ignoreCase = true))
        assertFalse(english.tagline.contains("Jagannath", ignoreCase = true))
        assertFalse(english.tagline.contains("Puri", ignoreCase = true))
        assertEquals("सहकार भारती", hindi.title)
        assertTrue(hindi.subtitle.contains("किसान"))
    }
}
