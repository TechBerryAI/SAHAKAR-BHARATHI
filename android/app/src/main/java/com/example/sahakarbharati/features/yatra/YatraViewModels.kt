package com.example.sahakarbharati.features.yatra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.repository.YatraRepository
import kotlinx.coroutines.launch

data class YatraListUi(
    val items: List<com.example.sahakarbharati.data.remote.YatraSummaryDto> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

class YatraListViewModel(
    private val repository: YatraRepository,
) : ViewModel() {
    var state by mutableStateOf(YatraListUi())
        private set

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = state.items.isEmpty(), error = null)
            repository.published(0).fold(
                onSuccess = { page ->
                    state = state.copy(
                        items = page.items,
                        total = page.total,
                        hasMore = page.hasMore,
                        loading = false,
                        error = null,
                    )
                },
                onFailure = { error -> state = state.copy(loading = false, error = error.message) },
            )
        }
    }

    fun loadMore() {
        if (state.loadingMore || state.loading || !state.hasMore) return
        val offset = state.items.size
        viewModelScope.launch {
            state = state.copy(loadingMore = true, error = null)
            repository.published(offset).fold(
                onSuccess = { page ->
                    val merged = (state.items + page.items).distinctBy { it.id }
                    state = state.copy(items = merged, total = page.total, hasMore = page.hasMore, loadingMore = false)
                },
                onFailure = { error -> state = state.copy(loadingMore = false, error = error.message) },
            )
        }
    }
}

data class YatraDetailUi(
    val yatra: YatraDetailDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class YatraDetailViewModel(
    private val repository: YatraRepository,
) : ViewModel() {
    var state by mutableStateOf(YatraDetailUi())
        private set

    fun load(yatraId: String) {
        viewModelScope.launch {
            state = state.copy(loading = state.yatra?.id != yatraId, error = null)
            repository.detail(yatraId).fold(
                onSuccess = { detail -> state = state.copy(yatra = detail, loading = false, error = null) },
                onFailure = { error -> state = state.copy(loading = false, error = error.message) },
            )
        }
    }
}

fun yatraStatusLabel(status: String): String = when (status) {
    "draft" -> "Draft"
    "planned" -> "Planned"
    "approval_pending" -> "Approval pending"
    "approved" -> "Approved"
    "upcoming" -> "Upcoming"
    "in_progress" -> "In progress"
    "completed" -> "Completed"
    "archived" -> "Archived"
    else -> status
}

fun yatraWhen(value: String): String = value.take(10)
