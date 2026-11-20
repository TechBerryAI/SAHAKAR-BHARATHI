package com.example.sahakarbharati.data

import androidx.core.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.ArrayDeque
import java.time.Instant
import kotlin.math.max

data class QueuedFix(
    val yatraId: String,
    val clientPositionId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double,
    val capturedAt: String,
    val credentialFingerprint: String,
)

interface FixQueue {
    fun snapshot(): List<QueuedFix>
    fun enqueue(fix: QueuedFix)
    fun remove(clientPositionId: String)
    fun discardFingerprint(fingerprint: String)
}

fun trackerFingerprint(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }.take(16)
}

/** Bounded offline queue. Evicts by age and cap; oldest unsent fix drops first. */
class PositionQueue(
    private val maxSize: Int = 1_440,
    private val maxAgeSeconds: Long = 6L * 60L * 60L,
) : FixQueue {
    private val items = ArrayDeque<QueuedFix>()

    override fun snapshot(): List<QueuedFix> {
        evict()
        return items.toList()
    }

    override fun enqueue(fix: QueuedFix) {
        evict()
        if (items.any { it.clientPositionId == fix.clientPositionId }) return
        while (items.size >= maxSize) items.removeFirst()
        items.addLast(fix)
    }

    override fun remove(clientPositionId: String) {
        items.removeAll { it.clientPositionId == clientPositionId }
    }

    override fun discardFingerprint(fingerprint: String) {
        items.removeAll { it.credentialFingerprint == fingerprint }
    }

    private fun evict() {
        val cutoff = Instant.now().epochSecond - maxAgeSeconds
        items.removeAll { fix ->
            runCatching { Instant.parse(fix.capturedAt).epochSecond < cutoff }.getOrDefault(false)
        }
    }
}

/** Same cap as [PositionQueue], written with AtomicFile so UI and service can share it. */
class FilePositionQueue(
    private val file: File,
    maxSize: Int = 1_440,
    maxAgeSeconds: Long = 6L * 60L * 60L,
) : FixQueue {
    private val memory = PositionQueue(maxSize, maxAgeSeconds)
    private val atomic = AtomicFile(file)

    init {
        if (file.exists()) {
            runCatching {
                atomic.readFully().toString(StandardCharsets.UTF_8).lineSequence().forEach { line ->
                    val parts = line.split('\t')
                    if (parts.size < 7) return@forEach
                    val latitude = parts[2].toDoubleOrNull() ?: return@forEach
                    val longitude = parts[3].toDoubleOrNull() ?: return@forEach
                    val accuracy = parts[4].toDoubleOrNull() ?: return@forEach
                    memory.enqueue(
                        QueuedFix(
                            yatraId = parts[0],
                            clientPositionId = parts[1],
                            latitude = latitude,
                            longitude = longitude,
                            accuracyM = accuracy,
                            capturedAt = parts[5],
                            credentialFingerprint = parts[6],
                        ),
                    )
                }
            }
        }
    }

    override fun snapshot(): List<QueuedFix> = memory.snapshot()

    override fun enqueue(fix: QueuedFix) {
        memory.enqueue(fix)
        write()
    }

    override fun remove(clientPositionId: String) {
        memory.remove(clientPositionId)
        write()
    }

    override fun discardFingerprint(fingerprint: String) {
        memory.discardFingerprint(fingerprint)
        write()
    }

    private fun write() {
        val body = memory.snapshot().joinToString("\n") { fix ->
            listOf(
                fix.yatraId,
                fix.clientPositionId,
                fix.latitude.toString(),
                fix.longitude.toString(),
                fix.accuracyM.toString(),
                fix.capturedAt,
                fix.credentialFingerprint,
            ).joinToString("\t")
        }
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(body.toByteArray(StandardCharsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            if (stream != null) atomic.failWrite(stream)
        }
    }
}

enum class FlushAction {
    Remove,
    Drop,
    Keep,
    ClearCredential,
    StopPermanent,
}

fun flushAction(status: Int): FlushAction = when (status) {
    200, 201 -> FlushAction.Remove
    400, 422 -> FlushAction.Drop
    401 -> FlushAction.ClearCredential
    404 -> FlushAction.StopPermanent
    else -> FlushAction.Keep
}

fun shouldCaptureFix(mocked: Boolean, ageMs: Long, intervalMs: Long, accuracyM: Float, maxAccuracyM: Float): Boolean {
    if (mocked) return false
    if (ageMs > intervalMs * 2) return false
    if (accuracyM > maxAccuracyM) return false
    return true
}

fun nextBackoffMs(previous: Long): Long = max(5_000L, previous * 2).coerceAtMost(5 * 60_000L)
