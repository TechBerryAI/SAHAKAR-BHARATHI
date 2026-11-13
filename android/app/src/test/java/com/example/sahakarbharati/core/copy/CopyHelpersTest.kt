package com.example.sahakarbharati.core.copy

import com.example.sahakarbharati.core.ui.occupationLabel
import com.example.sahakarbharati.data.model.ParticipantProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyHelpersTest {
    @Test
    fun hindiFamilyUsesHindiParticipantCopy() {
        assertTrue(AppLanguage.usesHindiCopy(AppLanguage.HI))
        assertTrue(AppLanguage.usesHindiCopy(AppLanguage.MAI))
        assertFalse(AppLanguage.usesHindiCopy(AppLanguage.EN))
        assertEquals("पंजीकृत प्रतिभागी", participantCopy(AppLanguage.HI).registeredParticipant)
        assertEquals("Registered participant", participantCopy(AppLanguage.EN).registeredParticipant)
        assertEquals(AppLanguage.HI, AppLanguage.fromKey("HI"))
    }

    @Test
    fun labelsStatusOccupationAndSmallProfileStrings() {
        val english = participantCopy(AppLanguage.EN)
        assertEquals("Active", statusLabel(english, "active"))
        assertEquals("Active", statusLabel(english, null))
        assertEquals("Disabled", statusLabel(english, "disabled"))
        assertEquals("pending", statusLabel(english, "pending"))

        val copy = copyFor(AppLanguage.EN)
        assertEquals(copy.farmer, occupationLabel(copy, "Farmer"))
        assertEquals(copy.fisherman, occupationLabel(copy, "Fisherman"))
        assertEquals(copy.both, occupationLabel(copy, "Both"))
        assertEquals(copy.allied, occupationLabel(copy, "Allied"))
        assertNull(occupationLabel(copy, ""))
        assertEquals("Teacher", occupationLabel(copy, "Teacher"))
        assertEquals(copy.title, copy.cloneCopy().title)
        assertEquals(copy.getStarted, copy.cloneCopy().getStarted)

        assertEquals("+91 9876543210", formatMobile("9876543210"))
        assertEquals("—", formatMobile("  "))
        assertEquals("AD", personInitials(" anita desai "))
        assertEquals("SB", personInitials("   "))
        assertEquals("Nashik, Maharashtra", regionLabel("Nashik", "Maharashtra"))
        assertEquals("", regionLabel(" ", null))
        assertEquals("Ravi Kumar", titleCase("  ravi   kumar "))
        assertNull(blankToNull("   "))
        assertEquals("Ward 4", blankToNull(" Ward 4 "))

        val profile = ParticipantProfile(
            loginId = "SB-1",
            fullName = "Anita Desai",
            mobile = "9876543210",
            district = "Nashik",
            state = "Maharashtra",
        )
        assertEquals("Anita", profile.firstName)
        assertEquals("Nashik • Maharashtra", profile.region)
    }
}
