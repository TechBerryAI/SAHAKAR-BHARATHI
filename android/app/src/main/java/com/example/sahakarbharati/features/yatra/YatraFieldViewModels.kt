package com.example.sahakarbharati.features.yatra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.CounterQr
import com.example.sahakarbharati.data.FixQueue
import com.example.sahakarbharati.data.FlushAction
import com.example.sahakarbharati.data.QueuedFix
import com.example.sahakarbharati.data.flushAction
import com.example.sahakarbharati.data.repository.PositionSubmit
import com.example.sahakarbharati.data.repository.TrackerCredentialStore
import com.example.sahakarbharati.data.repository.YatraFieldRepository
import com.example.sahakarbharati.data.shouldCaptureFix
import kotlinx.coroutines.launch
import java.util.UUID

class YatraAttendanceViewModel(
    private val repository: YatraFieldRepository,
) : ViewModel() {
    var outcome by mutableStateOf<AttendanceOutcome?>(null)
        private set

    fun scan(yatraId: String, raw: String) {
        val token = CounterQr.fromQr(raw)
        if (token == null) {
            outcome = AttendanceOutcome.Invalid("That QR code is not a counter.")
            return
        }
        outcome = AttendanceOutcome.Submitting
        viewModelScope.launch {
            outcome = attendanceOutcome(repository.scan(yatraId, raw.trim()))
        }
    }
}

class YatraTrackingViewModel(
    private val repository: YatraFieldRepository,
    private val credentials: TrackerCredentialStore,
    private val queue: FixQueue,
) : ViewModel() {
    var message by mutableStateOf<String?>(null)
        private set
    var pending by mutableStateOf(queue.snapshot().size)
        private set
    var sending by mutableStateOf(false)
        private set

    fun hasCredential(): Boolean = !credentials.trackerToken().isNullOrBlank()

    fun saveToken(token: String) {
        val cleaned = token.trim()
        if (cleaned.isEmpty()) {
            message = "Paste the tracker code from the Yatra desk."
            return
        }
        credentials.saveTrackerToken(cleaned)
        message = "Tracker code saved on this phone."
    }

    fun capture(yatraId: String, latitude: Double, longitude: Double, accuracyM: Double, mocked: Boolean, capturedAt: String) {
        if (!shouldCaptureFix(mocked)) {
            message = "A mock location is not sent."
            return
        }
        if (credentials.trackerToken().isNullOrBlank()) {
            message = "Paste the tracker code from the Yatra desk."
            return
        }
        val fix = QueuedFix(
            yatraId = yatraId,
            clientPositionId = UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            accuracyM = accuracyM,
            capturedAt = capturedAt,
        )
        queue.enqueue(fix)
        pending = queue.snapshot().size
        flush()
    }

    fun flush() {
        if (sending) return
        val batch = queue.snapshot()
        if (batch.isEmpty()) return
        sending = true
        viewModelScope.launch {
            for (fix in batch) {
                when (val result = repository.postPosition(fix)) {
                    is PositionSubmit.Accepted -> queue.remove(fix.clientPositionId)
                    is PositionSubmit.Http -> applyHttp(fix, result.status, result.message)
                    PositionSubmit.NoCredential -> message = "Paste the tracker code from the Yatra desk."
                }
            }
            pending = queue.snapshot().size
            sending = false
        }
    }

    private fun applyHttp(fix: QueuedFix, status: Int, detail: String) {
        when (flushAction(status)) {
            FlushAction.Remove, FlushAction.Drop -> queue.remove(fix.clientPositionId)
            FlushAction.ClearCredential -> {
                credentials.clearTrackerToken()
                message = detail
            }
            FlushAction.Keep -> message = detail
        }
    }
}
