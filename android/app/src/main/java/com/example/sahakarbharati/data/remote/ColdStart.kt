package com.example.sahakarbharati.data.remote

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/*
 * The API host suspends the service when idle; the first request afterwards can
 * take most of a minute while it starts. These interceptors keep the app usable
 * through that: safe requests are retried, and the UI is told when a request is
 * slow so it can say the server is waking up instead of looking frozen.
 */

private val RETRYABLE_STATUS = setOf(502, 503, 504)
private val SAFE_METHODS = setOf("GET", "HEAD")

/**
 * Retries GET/HEAD after a network failure or a 502/503/504 from the host's proxy.
 * Requests that change data (sign-in, registration, profile update, logout) are
 * never retried, so a request that reached the server is never repeated.
 */
class ColdStartRetryInterceptor(
    private val backoffMillis: List<Long> = listOf(2_000L, 5_000L),
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in SAFE_METHODS) {
            return chain.proceed(request)
        }
        var attempt = 0
        while (true) {
            val outcome = try {
                chain.proceed(request)
            } catch (error: IOException) {
                if (attempt >= backoffMillis.size || chain.call().isCanceled()) throw error
                null
            }
            if (outcome != null) {
                if (outcome.code !in RETRYABLE_STATUS || attempt >= backoffMillis.size) {
                    return outcome
                }
                outcome.close()
            }
            sleep(backoffMillis[attempt])
            attempt += 1
        }
    }
}

/**
 * Reports, as Compose state, whether any request has been in flight longer than
 * [thresholdMillis]. Install it first so it measures the whole call, retries included.
 */
class SlowRequestTracker(
    private val thresholdMillis: Long = 4_000L,
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "slow-request-tracker").apply { isDaemon = true }
    },
) : Interceptor {
    private val slowState = mutableStateOf(false)
    private var slowCalls = 0

    /** True while at least one request has been waiting longer than the threshold. */
    val serverSlow: State<Boolean>
        get() = slowState

    override fun intercept(chain: Interceptor.Chain): Response {
        val marked = BooleanArray(1)
        val pending = scheduler.schedule({ markSlow(marked) }, thresholdMillis, TimeUnit.MILLISECONDS)
        try {
            return chain.proceed(chain.request())
        } finally {
            pending.cancel(false)
            clearSlow(marked)
        }
    }

    private fun markSlow(marked: BooleanArray) = synchronized(this) {
        if (!marked[0]) {
            marked[0] = true
            slowCalls += 1
            slowState.value = true
        }
    }

    private fun clearSlow(marked: BooleanArray) = synchronized(this) {
        if (marked[0]) {
            slowCalls -= 1
            slowState.value = slowCalls > 0
        } else {
            // Keep a late timer from marking a call that already finished.
            marked[0] = true
        }
    }
}
