package com.example.sahakarbharati.features.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.SessionWatch
import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserSummary
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.repository.AdminRepository
import com.example.sahakarbharati.data.repository.IdleSession
import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.launch

class AdminUsersViewModel(
    private val adminRepository: AdminRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var users by mutableStateOf<List<AdminUserSummary>>(emptyList())
        private set
    var total by mutableStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var submitted = ""
    private var generation = 0
    private var refreshing = false
    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    /** Reloads page 1. Called when the People screen is opened, and after a search. */

    fun onQueryChange(value: String) {
        query = value
    }

    fun search() {
        submitted = query
        refresh()
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        val ticket = ++generation
        viewModelScope.launch {
            try {
                if (users.isEmpty()) loading = true
                error = null
                adminRepository.listUsers(submitted, 0).fold(
                    onSuccess = { page ->
                        if (ticket != generation) return@fold
                        users = page.items
                        total = page.total
                    },
                    onFailure = { failure ->
                        if (ticket == generation) error = failure.message
                    },
                )
            } finally {
                if (ticket == generation) loading = false
                refreshing = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || refreshing || users.size >= total) return
        val ticket = generation
        val offset = users.size
        loadingMore = true
        viewModelScope.launch {
            try {
                adminRepository.listUsers(submitted, offset).fold(
                    onSuccess = { page ->
                        if (ticket != generation) return@fold
                        val merged = (users + page.items).distinctBy { it.id }
                        users = merged
                        total = page.total
                    },
                    onFailure = { failure ->
                        if (ticket == generation) error = failure.message
                    },
                )
            }             finally {
                if (ticket == generation) loadingMore = false
            }
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        generation += 1
        refreshing = false
        submitted = ""
        query = ""
        users = emptyList()
        total = 0
        loading = false
        loadingMore = false
        error = null
    }
}

class AdminUserViewModel(
    private val adminRepository: AdminRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var person by mutableStateOf<AdminUserDetail?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set

    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    fun load(userId: String) {
        val started = watch.stamp()
        viewModelScope.launch {
            if (person?.id != userId) {
                person = null
                loading = true
            }
            adminRepository.getUser(userId).fold(
                onSuccess = {
                    if (!watch.still(started)) return@fold
                    person = it
                    error = null
                },
                onFailure = { failure ->
                    if (watch.still(started)) error = failure.message
                },
            )
            if (watch.still(started)) loading = false
        }
    }

    fun setRole(userId: String, role: String) {
        if (busy) return
        busy = true
        error = null
        val started = watch.stamp()
        viewModelScope.launch {
            try {
                adminRepository.setRole(userId, role).fold(
                    onSuccess = { if (watch.still(started)) person = it },
                    onFailure = { failure -> if (watch.still(started)) error = failure.message },
                )
            } finally {
                if (watch.still(started)) busy = false
            }
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        person = null
        error = null
        loading = false
        busy = false
    }
}

class AdminPlacesViewModel(
    private val adminRepository: AdminRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var places by mutableStateOf<List<PlaceDto>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set
    var total by mutableStateOf(0)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var generation = 0
    private var refreshing = false
    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    val hasMore: Boolean get() = places.size < total

    /** Reloads page 1. Called when the Booths screen is opened. */

    fun onNameChange(value: String) {
        name = value.take(120)
        if (error == NAME_REQUIRED) error = null
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        val ticket = ++generation
        viewModelScope.launch {
            try {
                if (places.isEmpty()) loading = true
                error = null
                adminRepository.listPlaces(0).fold(
                    onSuccess = { page ->
                        if (ticket != generation) return@fold
                        places = page.items
                        total = page.total
                    },
                    onFailure = { failure ->
                        if (ticket == generation) error = failure.message
                    },
                )
            } finally {
                if (ticket == generation) loading = false
                refreshing = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || refreshing || !hasMore) return
        val ticket = generation
        val offset = places.size
        loadingMore = true
        viewModelScope.launch {
            try {
                adminRepository.listPlaces(offset).fold(
                    onSuccess = { page ->
                        if (ticket != generation) return@fold
                        places = (places + page.items).distinctBy { it.id }
                        total = page.total
                    },
                    onFailure = { failure ->
                        if (ticket == generation) error = failure.message
                    },
                )
            } finally {
                if (ticket == generation) loadingMore = false
            }
        }
    }

    fun create() {
        if (saving) return
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            error = NAME_REQUIRED
            return
        }
        saving = true
        error = null
        val started = watch.stamp()
        viewModelScope.launch {
            try {
                adminRepository.createPlace(trimmed).fold(
                    onSuccess = { created ->
                        if (!watch.still(started)) return@fold
                        places = listOf(created) + places.filter { it.id != created.id }
                        if (places.size > total) total = places.size
                        name = ""
                    },
                    onFailure = { failure -> if (watch.still(started)) error = failure.message },
                )
            } finally {
                if (watch.still(started)) saving = false
            }
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        generation += 1
        refreshing = false
        name = ""
        places = emptyList()
        total = 0
        loading = false
        loadingMore = false
        saving = false
        error = null
    }

    private companion object {
        const val NAME_REQUIRED = "Booth name is required."
    }
}

class AdminPlaceViewModel(
    private val adminRepository: AdminRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var place by mutableStateOf<PlaceDto?>(null)
        private set
    var visits by mutableStateOf<List<AttendanceRecordDto>>(emptyList())
        private set
    var total by mutableStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    fun load(placeId: String) {
        val started = watch.stamp()
        viewModelScope.launch {
            if (place?.id != placeId) {
                place = null
                visits = emptyList()
                loading = true
            }
            error = null
            adminRepository.getPlace(placeId).fold(
                onSuccess = { found ->
                    if (!watch.still(started)) return@fold
                    place = found
                    adminRepository.listPlaceAttendance(found.id, 0).fold(
                        onSuccess = { attendance ->
                            if (!watch.still(started)) return@fold
                            visits = attendance.items
                            total = attendance.total
                        },
                        onFailure = { failure -> if (watch.still(started)) error = failure.message },
                    )
                },
                onFailure = { failure -> if (watch.still(started)) error = failure.message },
            )
            if (watch.still(started)) loading = false
        }
    }

    fun toggleActive() {
        val current = place ?: return
        if (busy) return
        busy = true
        error = null
        val started = watch.stamp()
        viewModelScope.launch {
            try {
                adminRepository.setPlaceActive(current.id, !current.active).fold(
                    onSuccess = { if (watch.still(started)) place = it },
                    onFailure = { failure -> if (watch.still(started)) error = failure.message },
                )
            } finally {
                if (watch.still(started)) busy = false
            }
        }
    }

    fun loadMore() {
        val current = place ?: return
        if (busy || visits.size >= total) return
        busy = true
        val offset = visits.size
        val started = watch.stamp()
        viewModelScope.launch {
            try {
                adminRepository.listPlaceAttendance(current.id, offset).fold(
                    onSuccess = { page ->
                        if (!watch.still(started)) return@fold
                        visits = (visits + page.items).distinctBy { it.id }
                        total = page.total
                    },
                    onFailure = { failure -> if (watch.still(started)) error = failure.message },
                )
            } finally {
                if (watch.still(started)) busy = false
            }
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        place = null
        visits = emptyList()
        total = 0
        loading = false
        busy = false
        error = null
    }
}
