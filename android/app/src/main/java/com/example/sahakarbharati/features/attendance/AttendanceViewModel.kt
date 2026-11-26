package com.example.sahakarbharati.features.attendance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.SessionWatch
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.repository.AttendanceRepository
import com.example.sahakarbharati.data.repository.IdleSession
import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.launch

data class AttendanceUi(
    val history: List<AttendanceRecordDto> = emptyList(),
    val latest: AttendanceRecordDto? = null,
    val total: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val hasMore: Boolean get() = history.size < total
}

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var state by mutableStateOf(AttendanceUi())
        private set

    private var inFlight = false
    private var paging = false
    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    fun load() {
        val started = watch.stamp()
        viewModelScope.launch {
            state = state.copy(loading = state.history.isEmpty(), error = null)
            val visits = repository.mine(0)
            if (!watch.still(started)) return@launch
            val page = visits.getOrNull()
            state = state.copy(
                history = page?.items ?: state.history,
                total = page?.total ?: state.total,
                loading = false,
                loadingMore = false,
                error = visits.exceptionOrNull()?.message,
            )
        }
    }

    fun loadMore() {
        if (paging || state.loading || !state.hasMore) return
        paging = true
        val started = watch.stamp()
        val offset = state.history.size
        state = state.copy(loadingMore = true, error = null)
        viewModelScope.launch {
            try {
                repository.mine(offset).fold(
                    onSuccess = { page ->
                        if (!watch.still(started)) return@fold
                        val merged = (state.history + page.items).distinctBy { it.id }
                        state = state.copy(history = merged, total = page.total, error = null)
                    },
                    onFailure = { error ->
                        if (watch.still(started)) state = state.copy(error = error.message)
                    },
                )
            } finally {
                paging = false
                if (watch.still(started)) state = state.copy(loadingMore = false)
            }
        }
    }

    fun checkIn(code: String, source: String) {
        if (inFlight) return
        inFlight = true
        val started = watch.stamp()
        viewModelScope.launch {
            state = state.copy(submitting = true, error = null)
            repository.checkIn(code, source).fold(
                onSuccess = { visit ->
                    if (!watch.still(started)) return@fold
                    val history = listOf(visit) + state.history.filter { it.id != visit.id }
                    state = state.copy(latest = visit, history = history, submitting = false, error = null)
                },
                onFailure = { error ->
                    if (watch.still(started)) state = state.copy(submitting = false, error = error.message)
                },
            )
            inFlight = false
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        inFlight = false
        paging = false
        state = AttendanceUi(loading = false)
    }
}
