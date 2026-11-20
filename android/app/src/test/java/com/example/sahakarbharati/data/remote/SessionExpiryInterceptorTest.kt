package com.example.sahakarbharati.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiryInterceptorTest {
    @Test
    fun trackerBearerDoesNotMatchTheSignedInUser() {
        assertTrue(bearerMatchesStored("Bearer user-token", "user-token"))
        assertFalse(bearerMatchesStored("Bearer tracker-secret", "user-token"))
        assertFalse(bearerMatchesStored(null, "user-token"))
    }
}
