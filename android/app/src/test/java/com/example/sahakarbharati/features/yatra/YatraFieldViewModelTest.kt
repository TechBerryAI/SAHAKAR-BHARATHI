package com.example.sahakarbharati.features.yatra

import com.example.sahakarbharati.data.PositionQueue
import com.example.sahakarbharati.data.QueuedFix
import com.example.sahakarbharati.data.remote.YatraAttendanceRecordDto
import com.example.sahakarbharati.data.repository.MemoryTrackerCredentialStore
import com.example.sahakarbharati.data.repository.PositionSubmit
import com.example.sahakarbharati.data.repository.YatraFieldRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YatraFieldViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scanShowsSuccessOnlyAfterTheServerAccepts() = runTest {
        val gate = CompletableDeferred<Result<YatraAttendanceRecordDto>>()
        val viewModel = YatraAttendanceViewModel(ScriptedFieldRepository(scanResult = { gate.await() }))
        viewModel.scan("yatra-1", "SB-COUNTER:abcdefghijklmnopqrstuvwxyz012345")
        assertEquals(AttendanceOutcome.Submitting, viewModel.outcome)
        gate.complete(Result.success(record(already = false)))
        advanceUntilIdle()
        assertEquals(AttendanceOutcome.Success, viewModel.outcome)
    }

    @Test
    fun duplicateScanIsNotANewSuccess() = runTest {
        val viewModel = YatraAttendanceViewModel(
            ScriptedFieldRepository(scanResult = { Result.success(record(already = true)) }),
        )
        viewModel.scan("yatra-1", "SB-COUNTER:abcdefghijklmnopqrstuvwxyz012345")
        advanceUntilIdle()
        assertEquals(AttendanceOutcome.Duplicate, viewModel.outcome)
    }

    @Test
    fun boothQrIsInvalidBeforeAnyRequest() {
        var called = false
        val viewModel = YatraAttendanceViewModel(
            ScriptedFieldRepository(scanResult = {
                called = true
                Result.success(record(already = false))
            }),
        )
        viewModel.scan("yatra-1", "SB-PLACE:123456")
        assertTrue(viewModel.outcome is AttendanceOutcome.Invalid)
        assertEquals(false, called)
    }

    @Test
    fun rejectedTrackerClearsOnlyTheTrackerCredential() = runTest {
        val credentials = MemoryTrackerCredentialStore()
        credentials.saveTrackerToken("tracker-secret")
        val queue = PositionQueue()
        val fix = QueuedFix("yatra-1", "11111111-1111-1111-1111-111111111111", 19.0, 73.0, 8.0, "2026-11-20T05:00:00Z")
        queue.enqueue(fix)
        val viewModel = YatraTrackingViewModel(
            ScriptedFieldRepository(position = { PositionSubmit.Http(401, "Session is invalid or expired.") }),
            credentials,
            queue,
        )
        viewModel.flush()
        advanceUntilIdle()
        assertNull(credentials.trackerToken())
        assertEquals(listOf(fix.clientPositionId), queue.snapshot().map { it.clientPositionId })
    }

    @Test
    fun acceptedFixLeavesTheQueueAndARetryKeepsItsId() = runTest {
        val credentials = MemoryTrackerCredentialStore()
        credentials.saveTrackerToken("tracker-secret")
        val queue = PositionQueue()
        val fix = QueuedFix("yatra-1", "22222222-2222-2222-2222-222222222222", 19.0, 73.0, 8.0, "2026-11-20T05:00:00Z")
        queue.enqueue(fix)
        var posts = 0
        val viewModel = YatraTrackingViewModel(
            ScriptedFieldRepository(position = {
                posts += 1
                if (posts == 1) PositionSubmit.Http(429, "Too many attempts. Try again later.")
                else PositionSubmit.Accepted(200)
            }),
            credentials,
            queue,
        )
        viewModel.flush()
        advanceUntilIdle()
        assertEquals(listOf(fix.clientPositionId), queue.snapshot().map { it.clientPositionId })
        viewModel.flush()
        advanceUntilIdle()
        assertEquals(emptyList<String>(), queue.snapshot().map { it.clientPositionId })
        assertEquals("tracker-secret", credentials.trackerToken())
    }

    private fun record(already: Boolean) = YatraAttendanceRecordDto(
        id = "attendance-1",
        yatraId = "yatra-1",
        counterId = "counter-1",
        counterName = "Gate",
        attendanceDate = "2026-11-20",
        checkedInAt = "2026-11-20T05:00:00Z",
        alreadyRecorded = already,
    )
}

private class ScriptedFieldRepository(
    private val scanResult: suspend () -> Result<YatraAttendanceRecordDto> = { error("unused") },
    private val position: suspend () -> PositionSubmit = { PositionSubmit.NoCredential },
) : YatraFieldRepository {
    override suspend fun scan(yatraId: String, qrText: String): Result<YatraAttendanceRecordDto> = scanResult()

    override suspend fun postPosition(fix: QueuedFix): PositionSubmit = position()
}
