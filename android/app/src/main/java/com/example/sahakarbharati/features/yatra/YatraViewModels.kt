package com.example.sahakarbharati.features.yatra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.copy.ui
import com.example.sahakarbharati.data.SessionWatch
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.remote.YatraStopDto
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.repository.IdleSession
import com.example.sahakarbharati.data.repository.SessionStore
import com.example.sahakarbharati.data.repository.YatraCatalogPage
import com.example.sahakarbharati.data.repository.YatraRepository
import kotlinx.coroutines.launch

data class YatraListUi(
    val items: List<YatraSummaryDto> = emptyList(),
    val offset: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val nextPageError: String? = null,
)

class YatraListViewModel(
    private val repository: YatraRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var state by mutableStateOf(YatraListUi())
        private set

    private var generation = 0
    private var paging = false
    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    fun load() {
        paging = false
        val started = watch.stamp()
        val request = ++generation
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, nextPageError = null)
            val result = repository.published(0)
            if (!watch.still(started) || request != generation) return@launch
            result.fold(
                onSuccess = { page -> state = page.appliedOver(page.items) },
                onFailure = { error ->
                    state = state.copy(loading = false, loadingMore = false, error = error.message)
                },
            )
        }
    }

    fun loadMore() {
        if (paging || state.loading || state.loadingMore || !state.hasMore) return
        paging = true
        val started = watch.stamp()
        val request = ++generation
        val offset = state.offset
        viewModelScope.launch {
            state = state.copy(loadingMore = true, nextPageError = null)
            try {
                val result = repository.published(offset)
                if (!watch.still(started) || request != generation) return@launch
                result.fold(
                    onSuccess = { page ->
                        val seen = state.items.map { it.id }.toSet()
                        val merged = state.items + page.items.filter { it.id !in seen }
                        state = page.appliedOver(merged)
                    },
                    onFailure = { error ->
                        state = state.copy(loadingMore = false, nextPageError = error.message)
                    },
                )
            } finally {
                if (watch.still(started) && request == generation) paging = false
            }
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        generation += 1
        paging = false
        state = YatraListUi(loading = false)
    }

    private fun YatraCatalogPage.appliedOver(items: List<YatraSummaryDto>) = YatraListUi(
        items = items,
        offset = offset + received,
        loading = false,
        loadingMore = false,
        hasMore = hasMore,
    )
}

data class YatraDetailUi(
    val yatra: YatraDetailDto? = null,
    val requestedId: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class YatraDetailViewModel(
    private val repository: YatraRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var state by mutableStateOf(YatraDetailUi())
        private set

    private var generation = 0
    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    fun load(yatraId: String) {
        val started = watch.stamp()
        val request = ++generation
        viewModelScope.launch {
            state = state.copy(requestedId = yatraId, loading = true, error = null)
            val result = repository.detail(yatraId)
            if (!watch.still(started) || request != generation) return@launch
            result.fold(
                onSuccess = { detail ->
                    state = state.copy(yatra = detail, requestedId = yatraId, loading = false, error = null)
                },
                onFailure = { error ->
                    state = state.copy(
                        yatra = state.yatra?.takeIf { it.id == yatraId },
                        requestedId = yatraId,
                        loading = false,
                        error = error.message,
                    )
                },
            )
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        generation += 1
        state = YatraDetailUi(loading = false)
    }
}

fun yatraStatusKey(status: String): String = when (status) {
    "draft" -> "statusDraft"
    "planned" -> "statusPlanned"
    "approval_pending" -> "statusApprovalPending"
    "approved" -> "statusApproved"
    "upcoming" -> "statusUpcoming"
    "in_progress" -> "statusInProgress"
    "completed" -> "statusCompleted"
    "archived" -> "statusArchived"
    else -> "yatraUnavailable"
}

@Composable
fun yatraStatusLabel(status: String): String = ui(yatraStatusKey(status))

/** The clock digits already present on the API timestamp. No timezone conversion. */
fun yatraClock(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val marker = value.indexOf('T')
    if (marker < 0 || value.length < marker + 6) return null
    val clock = value.substring(marker + 1, marker + 6)
    return clock.takeIf { it[2] == ':' }
}

fun yatraWhen(value: String): String = value.take(10)

@Composable
fun yatraDateRange(start: String, end: String): String =
    ui("yatraDateRange", mapOf("start" to yatraWhen(start), "end" to yatraWhen(end)))

@Composable
fun yatraRowLabel(yatra: YatraSummaryDto): String = ui(
    "yatraRowLabel",
    mapOf(
        "name" to yatra.name,
        "district" to yatra.district.name,
        "when" to yatraDateRange(yatra.startsAt, yatra.endsAt),
        "status" to yatraStatusLabel(yatra.status),
    ),
)

@Composable
fun stopTimeLabel(stop: YatraStopDto): String? {
    val arrival = yatraClock(stop.plannedArrival)
    val departure = yatraClock(stop.plannedDeparture)
    val parts = listOfNotNull(
        arrival?.let { "${ui("arrival")} $it" },
        departure?.let { "${ui("departure")} $it" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(". ")
}
