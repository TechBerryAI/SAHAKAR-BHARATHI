package com.example.sahakarbharati.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.util.UUID

class PositionQueueTest {
    @Test
    fun dropsOldestAtFiftyAndKeepsTheSameClientId() {
        val queue = PositionQueue(maxSize = 50)
        val first = fix("yatra", "00000000-0000-0000-0000-000000000001")
        queue.enqueue(first)
        repeat(50) { index ->
            queue.enqueue(fix("yatra", "00000000-0000-0000-0000-${(index + 10).toString().padStart(12, '0')}"))
        }
        val stored = queue.snapshot()
        assertEquals(50, stored.size)
        assertFalse(stored.any { it.clientPositionId == first.clientPositionId })
        queue.enqueue(first)
        assertEquals(50, queue.snapshot().size)
        assertEquals(1, queue.snapshot().count { it.clientPositionId == first.clientPositionId })
    }

    @Test
    fun fileQueueReloadsTheSameClientId() {
        val file = File.createTempFile("positions", ".txt")
        val id = UUID.randomUUID().toString()
        FilePositionQueue(file).enqueue(fix("yatra-1", id))
        val reloaded = FilePositionQueue(file).snapshot()
        assertEquals(listOf(id), reloaded.map { it.clientPositionId })
        file.delete()
    }

    @Test
    fun flushKeepsRetryAndDropsPoison() {
        assertEquals(FlushAction.Remove, flushAction(201))
        assertEquals(FlushAction.Remove, flushAction(200))
        assertEquals(FlushAction.Keep, flushAction(429))
        assertEquals(FlushAction.Keep, flushAction(0))
        assertEquals(FlushAction.Drop, flushAction(400))
        assertEquals(FlushAction.Drop, flushAction(422))
        assertEquals(FlushAction.ClearCredential, flushAction(401))
        assertFalse(shouldCaptureFix(mocked = true))
    }

    private fun fix(yatraId: String, id: String) = QueuedFix(
        yatraId = yatraId,
        clientPositionId = id,
        latitude = 19.9,
        longitude = 73.7,
        accuracyM = 12.0,
        capturedAt = "2026-11-20T05:00:00Z",
    )
}
