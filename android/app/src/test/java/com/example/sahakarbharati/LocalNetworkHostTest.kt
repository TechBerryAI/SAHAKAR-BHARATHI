package com.example.sahakarbharati

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkHostTest {
    @Test
    fun emulatorHostNeedsLocalNetworkAccess() {
        assertTrue(hostNeedsLocalNetwork("http://10.0.2.2:18000/"))
        assertTrue(hostNeedsLocalNetwork("http://192.168.1.20:8000/"))
        assertTrue(hostNeedsLocalNetwork("http://172.16.0.4:8000/"))
    }

    @Test
    fun loopbackAndPublicHostsDoNot() {
        assertFalse(hostNeedsLocalNetwork("http://127.0.0.1:8000/"))
        assertFalse(hostNeedsLocalNetwork("http://localhost:8000/"))
        assertFalse(hostNeedsLocalNetwork("https://sahakar-bharathi-api.onrender.com/"))
    }
}
