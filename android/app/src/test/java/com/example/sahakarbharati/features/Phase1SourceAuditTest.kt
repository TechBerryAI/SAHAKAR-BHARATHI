package com.example.sahakarbharati.features

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the Phase 1 stabilization rules that are easiest to regress in source. */
class Phase1SourceAuditTest {
    @Test
    fun pinFieldsAreNotSavedWithTheActivity() {
        val registration = source("features/registration/RegistrationFormScreen.kt")
        val signIn = source("features/registration/SignInScreen.kt")
        assertTrue(registration.contains("var pin by remember(accountEpoch) { mutableStateOf(\"\") }"))
        assertTrue(registration.contains("var confirmPin by remember(accountEpoch) { mutableStateOf(\"\") }"))
        assertFalse(registration.contains("var pin by rememberSaveable"))
        assertFalse(registration.contains("var confirmPin by rememberSaveable"))
        assertTrue(signIn.contains("var pin by remember(accountEpoch) { mutableStateOf(\"\") }"))
        assertFalse(signIn.contains("var pin by rememberSaveable"))
    }

    @Test
    fun registrationViewModelDoesNotHardcodeOtpSentences() {
        val source = source("features/registration/RegistrationViewModel.kt")
        listOf(
            "OTP sent",
            "Mobile number verified",
            "Send an OTP before verifying",
            "Enter the 6-digit code from the SMS",
            "Could not send the OTP",
            "Could not verify the OTP",
        ).forEach { sentence ->
            assertFalse(sentence, source.contains(sentence))
        }
        assertTrue(source.contains("\"otpSent\""))
        assertTrue(source.contains("\"otpVerified\""))
        assertTrue(source.contains("\"otpSendFirst\""))
        assertTrue(source.contains("\"otpRequired\""))
    }

    @Test
    fun removedRoutesAreNotPublicDestinations() {
        val source = source("ui/navigation/RathYatraDestinations.kt")
        assertFalse(source.contains("WELCOME"))
        assertFalse(source.contains("REGISTRATION_INTRO"))
    }

    private fun source(relative: String): String {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            val candidates = listOf(
                File(dir, "src/main/java/com/example/sahakarbharati/$relative"),
                File(dir, "app/src/main/java/com/example/sahakarbharati/$relative"),
                File(dir, "android/app/src/main/java/com/example/sahakarbharati/$relative"),
            )
            candidates.firstOrNull { it.isFile }?.let { return it.readText() }
            dir = dir.parentFile ?: return@repeat
        }
        throw AssertionError("Missing $relative from ${File(".").absolutePath}")
    }
}
