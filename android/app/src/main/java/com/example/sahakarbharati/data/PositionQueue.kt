package com.example.sahakarbharati.data

import java.io.File
import java.util.ArrayDeque

data class QueuedFix(
    val yatraId: String,
    val clientPositionId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double,
    val capturedAt: String,
)

interface FixQueue {
    fun snapshot(): List<QueuedFix>
    fun enqueue(fix: QueuedFix)
    fun remove(clientPositionId: String)
}

/** Bounded offline queue. At the cap, the oldest unsent fix is dropped. */
class PositionQueue(private val maxSize: Int = 50) : FixQueue {
    private val items = ArrayDeque<QueuedFix>()

    override fun snapshot(): List<QueuedFix> = items.toList()

    override fun enqueue(fix: QueuedFix) {
        if (items.any { it.clientPositionId == fix.clientPositionId }) return
        while (items.size >= maxSize) items.removeFirst()
        items.addLast(fix)
    }

    override fun remove(clientPositionId: String) {
        items.removeAll { it.clientPositionId == clientPositionId }
    }
}

/** Same cap as [PositionQueue], written to a file so a closed app can retry the same fix. */
class FilePositionQueue(private val file: File, maxSize: Int = 50) : FixQueue {
    private val memory = PositionQueue(maxSize)

    init {
        if (file.exists()) {
            file.readLines().forEach { line ->
                val parts = line.split('\t')
                if (parts.size != 6) return@forEach
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
                    ),
                )
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

    private fun write() {
        file.parentFile?.mkdirs()
        val body = memory.snapshot().joinToString("\n") { fix ->
            listOf(
                fix.yatraId,
                fix.clientPositionId,
                fix.latitude.toString(),
                fix.longitude.toString(),
                fix.accuracyM.toString(),
                fix.capturedAt,
            ).joinToString("\t")
        }
        file.writeText(body)
    }
}

enum class FlushAction {
    Remove,
    Drop,
    Keep,
    ClearCredential,
}

fun flushAction(status: Int): FlushAction = when (status) {
    200, 201 -> FlushAction.Remove
    400, 422 -> FlushAction.Drop
    401 -> FlushAction.ClearCredential
    else -> FlushAction.Keep
}

fun shouldCaptureFix(mocked: Boolean): Boolean = !mocked
