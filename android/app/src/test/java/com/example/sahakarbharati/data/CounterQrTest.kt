package com.example.sahakarbharati.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterQrTest {
    private val token = "abcdefghijklmnopqrstuvwxyz012345"

    @Test
    fun acceptsCounterPayloadOnly() {
        assertEquals(token, CounterQr.fromQr("SB-COUNTER:$token"))
        assertEquals(token, CounterQr.fromQr("  SB-COUNTER:$token  "))
        assertEquals("SB-COUNTER:$token", CounterQr.text(token))
    }

    @Test
    fun rejectsBoothCodesAndShortTokens() {
        assertNull(CounterQr.fromQr("SB-PLACE:123456"))
        assertNull(CounterQr.fromQr("SB-COUNTER:short"))
        assertNull(CounterQr.fromQr(token))
    }
}
