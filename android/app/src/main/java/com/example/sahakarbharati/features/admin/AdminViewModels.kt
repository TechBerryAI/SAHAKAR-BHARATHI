package com.example.sahakarbharati.features.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserSummary
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.repository.AdminRepository
import kotlinx.coroutines.launch

class AdminUsersViewModel(
    private val adminRepository: AdminRepository,
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

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        query = value
    }

    fun search() {
        submitted = query
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            error = null
            adminRepository.listUsers(submitted, 0).fold(
                onSuccess = { page ->
                    users = page.items
                    total = page.total
                },
                onFailure = { failure -> error = failure.message },
            )
            loading = false
        }
    }

    fun loadMore() {
        if (loadingMore || users.size >= total) return
        viewModelScope.launch {
            loadingMore = true
            adminRepository.listUsers(submitted, users.size).fold(
                onSuccess = { page ->
                    users = users + page.items
                    total = page.total
                },
                onFailure = { failure -> error = failure.message },
            )
            loadingMore = false
        }
    }
}

class AdminUserViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    var person by mutableStateOf<AdminUserDetail?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set

    fun load(userId: String) {
        viewModelScope.launch {
            if (person?.id != userId) {
                person = null
                loading = true
            }
            adminRepository.getUser(userId).fold(
                onSuccess = {
                    person = it
                    error = null
                },
                onFailure = { failure -> error = failure.message },
            )
            loading = false
        }
    }

    fun setRole(userId: String, role: String) {
        viewModelScope.launch {
            busy = true
            error = null
            adminRepository.setRole(userId, role).fold(
                onSuccess = { person = it },
                onFailure = { failure -> error = failure.message },
            )
            busy = false
        }
    }
}

class AdminPlacesViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var places by mutableStateOf<List<PlaceDto>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun onNameChange(value: String) {
        name = value.take(120)
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            error = null
            adminRepository.listPlaces().fold(
                onSuccess = { places = it },
                onFailure = { failure -> error = failure.message },
            )
            loading = false
        }
    }

    fun create() {
        val trimmed = name.trim()
        if (trimmed.length < 2 || saving) return
        viewModelScope.launch {
            saving = true
            error = null
            adminRepository.createPlace(trimmed).fold(
                onSuccess = { created ->
                    places = listOf(created) + places.filter { it.id != created.id }
                    name = ""
                },
                onFailure = { failure -> error = failure.message },
            )
            saving = false
        }
    }
}

class AdminPlaceViewModel(
    private val adminRepository: AdminRepository,
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

    fun load(placeId: String) {
        viewModelScope.launch {
            if (place?.id != placeId) {
                place = null
                visits = emptyList()
                loading = true
            }
            error = null
            adminRepository.listPlaces().fold(
                onSuccess = { page ->
                    val found = page.find { it.id == placeId }
                    place = found
                    if (found == null) {
                        error = "That booth was not found."
                    } else {
                        adminRepository.listPlaceAttendance(found.id, 0).fold(
                            onSuccess = { attendance ->
                                visits = attendance.items
                                total = attendance.total
                            },
                            onFailure = { failure -> error = failure.message },
                        )
                    }
                },
                onFailure = { failure -> error = failure.message },
            )
            loading = false
        }
    }

    fun toggleActive() {
        val current = place ?: return
        if (busy) return
        viewModelScope.launch {
            busy = true
            error = null
            adminRepository.setPlaceActive(current.id, !current.active).fold(
                onSuccess = { place = it },
                onFailure = { failure -> error = failure.message },
            )
            busy = false
        }
    }

    fun loadMore() {
        val current = place ?: return
        if (busy || visits.size >= total) return
        viewModelScope.launch {
            busy = true
            adminRepository.listPlaceAttendance(current.id, visits.size).fold(
                onSuccess = { page ->
                    visits = visits + page.items
                    total = page.total
                },
                onFailure = { failure -> error = failure.message },
            )
            busy = false
        }
    }
}
