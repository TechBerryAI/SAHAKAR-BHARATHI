package com.example.sahakarbharati.features.attendance

import org.junit.Assert.assertEquals
import org.junit.Test

class VisitTimeTest {
    @Test
    fun formatsAnInstantInIndiaAndKeepsTheFallbackDate() {
        assertEquals("3 Nov 2026, 3:43 PM", formatVisitTime("2026-11-03T10:13:00Z", "2026-11-03"))
        assertEquals("2026-11-03", formatVisitTime("not-a-time", "2026-11-03"))
    }
}
