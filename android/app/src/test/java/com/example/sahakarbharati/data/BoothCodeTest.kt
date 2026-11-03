package com.example.sahakarbharati.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoothCodeTest {
    @Test
    fun qrPayload_acceptsOnlyTheBoothPrefix() {
        assertEquals("004218", BoothCode.fromQr("SB-PLACE:004218"))
        assertEquals("004218", BoothCode.fromQr("  SB-PLACE:004218\n"))
        assertNull(BoothCode.fromQr("https://example.com/004218"))
        assertNull(BoothCode.fromQr("SB-PLACE:4218"))
        assertNull(BoothCode.fromQr("004218"))
    }

    @Test
    fun manualEntry_isSixDigits() {
        assertEquals("004218", BoothCode.fromManual("004218"))
        assertNull(BoothCode.fromManual("4218"))
        assertNull(BoothCode.fromManual("SB-PLACE:004218"))
        assertNull(BoothCode.fromManual("12 4218"))
    }

    @Test
    fun printedQr_usesTheBoothPrefix() {
        assertEquals("SB-PLACE:004218", BoothCode.text("004218"))
        assertEquals("004218", BoothCode.fromQr(BoothCode.text("004218")))
    }
}
