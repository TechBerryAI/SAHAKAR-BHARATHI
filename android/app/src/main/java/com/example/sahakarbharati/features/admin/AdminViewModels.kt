package com.example.sahakarbharati.features.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserSummary
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
