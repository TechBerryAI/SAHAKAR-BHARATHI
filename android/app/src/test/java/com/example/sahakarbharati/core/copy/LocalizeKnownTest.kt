package com.example.sahakarbharati.core.copy

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizeKnownTest {
    @Test
    fun otpAndRateLimitSentencesUseTheCatalog() {
        assertEquals(
            "Could not send the OTP.",
            localizeKnown(AppLanguage.EN, "Could not send the SMS. Please try again."),
        )
        assertEquals(
            "Please try again.",
            localizeKnown(AppLanguage.EN, "Please try again in a moment."),
        )
        assertEquals(
            "The OTP is incorrect or has expired.",
            localizeKnown(AppLanguage.EN, "The code is incorrect or has expired."),
        )
        assertEquals(
            "Too many attempts. Try again later.",
            localizeKnown(AppLanguage.EN, "Too many attempts. Try again later."),
        )
        assertEquals("कृपया फिर कोशिश करें।", localizeKnown(AppLanguage.HI, "Please try again in a moment."))
    }
}
