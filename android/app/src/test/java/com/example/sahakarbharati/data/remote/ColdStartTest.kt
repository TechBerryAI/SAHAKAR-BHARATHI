package com.example.sahakarbharati.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ColdStartTest {

    @Test
    fun getIsRetriedThroughProxyErrorsUntilTheServerAnswers() {
        val server = ScriptedServer(503, 502, 200)
        val sleeps = mutableListOf<Long>()
        val response = client(server, retry = ColdStartRetryInterceptor(listOf(10L, 20L)) { sleeps += it })
            .newCall(get()).execute()
        assertEquals(200, response.code)
        assertEquals(3, server.calls)
        assertEquals(listOf(10L, 20L), sleeps)
        response.close()
    }

    @Test
    fun getIsRetriedAfterATimeout() {
        val server = ScriptedServer(SocketTimeoutException("cold start"), 200)
        val response = client(server, retry = ColdStartRetryInterceptor(listOf(1L)) {}).newCall(get()).execute()
        assertEquals(200, response.code)
        assertEquals(2, server.calls)
        response.close()
    }

    @Test
    fun retriesStopAfterTheLastBackoffAndReturnTheFailure() {
        val server = ScriptedServer(503, 503, 503, 200)
        val response = client(server, retry = ColdStartRetryInterceptor(listOf(1L, 1L)) {}).newCall(get()).execute()
        assertEquals(503, response.code)
        assertEquals(3, server.calls)
        response.close()
    }

    @Test
    fun persistentNetworkFailureIsReportedNotSwallowed() {
        val server = ScriptedServer(IOException("down"), IOException("down"))
        try {
            client(server, retry = ColdStartRetryInterceptor(listOf(1L)) {}).newCall(get()).execute()
            fail("expected IOException")
        } catch (_: IOException) {
            assertEquals(2, server.calls)
        }
    }

    @Test
    fun requestsThatChangeDataAreNeverRetried() {
        listOf("POST", "PATCH", "DELETE").forEach { method ->
            val server = ScriptedServer(503, 200)
            val body = if (method == "DELETE") null else "{}".toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(URL).method(method, body).build()
            val response = client(server, retry = ColdStartRetryInterceptor(listOf(1L)) {}).newCall(request).execute()
            assertEquals(method, 503, response.code)
            assertEquals(method, 1, server.calls)
            response.close()
        }
    }

    @Test
    fun notFoundAndUnauthorizedAreNotRetried() {
        listOf(401, 404, 500).forEach { code ->
            val server = ScriptedServer(code, 200)
            val response = client(server, retry = ColdStartRetryInterceptor(listOf(1L)) {}).newCall(get()).execute()
            assertEquals(code, response.code)
            assertEquals(1, server.calls)
            response.close()
        }
    }

    @Test
    fun slowRequestRaisesTheWakingFlagUntilItFinishes() {
        val tracker = SlowRequestTracker(thresholdMillis = 50)
        val release = CountDownLatch(1)
        val server = ScriptedServer(200, beforeAnswer = { release.await(5, TimeUnit.SECONDS) })
        val call = thread { client(server, tracker = tracker).newCall(get()).execute().close() }

        waitUntil { tracker.serverSlow.value }
        assertTrue(tracker.serverSlow.value)
        release.countDown()
        call.join(5_000)
        waitUntil { !tracker.serverSlow.value }
        assertFalse(tracker.serverSlow.value)
    }

    @Test
    fun fastRequestNeverRaisesTheWakingFlag() {
        val tracker = SlowRequestTracker(thresholdMillis = 300)
        client(ScriptedServer(200), tracker = tracker).newCall(get()).execute().close()
        Thread.sleep(400)
        assertFalse(tracker.serverSlow.value)
    }

    @Test
    fun wakingFlagStaysUpWhileAnySlowRequestIsPending() {
        val tracker = SlowRequestTracker(thresholdMillis = 30)
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)
        val a = thread { client(ScriptedServer(200, beforeAnswer = { first.await(5, TimeUnit.SECONDS) }), tracker = tracker).newCall(get()).execute().close() }
        val b = thread { client(ScriptedServer(200, beforeAnswer = { second.await(5, TimeUnit.SECONDS) }), tracker = tracker).newCall(get()).execute().close() }
        waitUntil { tracker.serverSlow.value }
        Thread.sleep(100)
        first.countDown()
        a.join(5_000)
        assertTrue("one slow request still pending", tracker.serverSlow.value)
        second.countDown()
        b.join(5_000)
        waitUntil { !tracker.serverSlow.value }
        assertFalse(tracker.serverSlow.value)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
    }

    private fun get() = Request.Builder().url(URL).get().build()

    private fun client(
        server: ScriptedServer,
        retry: ColdStartRetryInterceptor? = null,
        tracker: SlowRequestTracker? = null,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        tracker?.let { builder.addInterceptor(it) }
        retry?.let { builder.addInterceptor(it) }
        return builder.addInterceptor(server).build()
    }

    private companion object {
        const val URL = "https://api.test.invalid/api/v1/me"
    }
}

/** Answers each attempt locally with the next scripted status or exception; no network. */
private class ScriptedServer(
    vararg replies: Any,
    private val beforeAnswer: () -> Unit = {},
) : Interceptor {
    private val queue = ArrayDeque(replies.toList())
    var calls = 0
        private set

    override fun intercept(chain: Interceptor.Chain): Response {
        calls += 1
        beforeAnswer()
        val reply = queue.removeFirstOrNull() ?: error("unexpected request ${chain.request().url}")
        if (reply is IOException) throw reply
        val code = reply as Int
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("status $code")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
