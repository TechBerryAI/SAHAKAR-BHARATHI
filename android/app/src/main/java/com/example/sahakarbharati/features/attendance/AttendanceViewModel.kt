package com.example.sahakarbharati.features.attendance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.repository.AttendanceRepository
import kotlinx.coroutines.launch

data class AttendanceUi(
    val history: List<AttendanceRecordDto> = emptyList(),
    val latest: AttendanceRecordDto? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
)

class AttendanceViewModel(
    private val repository: AttendanceRepository,
) : ViewModel() {
    var state by mutableStateOf(AttendanceUi())
        private set

    private var inFlight = false

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = state.history.isEmpty(), error = null)
            repository.mine().fold(
                onSuccess = { page -> state = state.copy(history = page.items, loading = false, error = null) },
                onFailure = { error -> state = state.copy(loading = false, error = error.message) },
            )
        }
    }

    fun checkIn(code: String, source: String) {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            state = state.copy(submitting = true, error = null)
            repository.checkIn(code, source).fold(
                onSuccess = { visit ->
                    val history = listOf(visit) + state.history.filter { it.id != visit.id }
                    state = state.copy(latest = visit, history = history, submitting = false, error = null)
                },
                onFailure = { error ->
                    state = state.copy(submitting = false, error = error.message)
                },
            )
            inFlight = false
        }
    }
}
