package com.example.sahakarbharati.features.registration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RegistrationRulesTest {
    @Test
    fun mobileAndDateOfBirthRules() {
        assertTrue(isIndianMobile("9876543210"))
        assertFalse(isIndianMobile("5876543210"))
        assertEquals("1985-03-07", isoDob("07/03/1985"))
        assertNull(isoDob("7/3/1985"))
        assertEquals("07/03/1985", joinDob("07", "03", "1985"))

        val millis = parseDobToUtcMillis("07 / 03 / 1985")
        assertTrue(millis != null)
        assertEquals("07 / 03 / 1985", formatDob(millis!!))
        assertNull(parseDobToUtcMillis("32/02/1985"))
        assertFalse(adultDob("31/02/1985"))
        assertFalse(adultDob("not-a-date"))
    }

    @Test
    fun ageUsesTheBirthdayAndRequiresEighteen() {
        val today = Calendar.getInstance()
        val adult = "%02d/%02d/%04d".format(
            today.get(Calendar.DAY_OF_MONTH),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.YEAR) - 30,
        )
        val justEighteen = "%02d/%02d/%04d".format(
            today.get(Calendar.DAY_OF_MONTH),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.YEAR) - 18,
        )
        val tomorrow = today.clone() as Calendar
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)
        val birthdayTomorrow = "%02d/%02d/%04d".format(
            tomorrow.get(Calendar.DAY_OF_MONTH),
            tomorrow.get(Calendar.MONTH) + 1,
            tomorrow.get(Calendar.YEAR) - 18,
        )
        assertEquals(30, ageYearsFromDob(adult))
        assertTrue(adultDob(justEighteen))
        assertFalse(adultDob(birthdayTomorrow))
        assertEquals(0, ageYearsFromDob("bad"))
    }
}
