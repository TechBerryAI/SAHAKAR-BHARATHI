package com.example.sahakarbharati.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorMappingTest {
    @Test
    fun mapsNetworkPrimitiveDetailBlankBodyAndApiException() {
        assertEquals(
            "The server could not be reached. Check your connection.",
            mapApiError(IOException("down"), "fallback"),
        )
        assertEquals(
            "That booth was not found.",
            mapApiError(http(404, """{"detail":"That booth was not found."}"""), "fallback"),
        )
        assertEquals("fallback", mapApiError(http(500, ""), "fallback"))
        assertEquals("fallback", mapApiError(http(500, "not-json"), "fallback"))
        assertEquals("Already registered", mapApiError(ApiException("Already registered"), "fallback"))
        assertEquals("fallback", mapApiError(IllegalStateException("nope"), "fallback"))
    }

    private fun http(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
