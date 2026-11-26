package com.example.sahakarbharati.features.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.SessionWatch
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.IdleSession
import com.example.sahakarbharati.data.repository.ProfileRepository
import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var state by mutableStateOf<UiState<ParticipantProfile>>(UiState.Idle)
        private set
    var logoutState by mutableStateOf<UiState<Unit>>(UiState.Idle)
        private set

    var saveError by mutableStateOf<String?>(null)
        private set
    var saved by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var refreshError by mutableStateOf<String?>(null)
        private set

    private val watch = SessionWatch(session, viewModelScope, onChange = ::clearAccount)

    fun load() {
        val started = watch.stamp()
        viewModelScope.launch {
            val previous = state
            if (previous !is UiState.Success) {
                state = UiState.Loading
            }
            profileRepository.me().fold(
                onSuccess = {
                    if (!watch.still(started)) return@fold
                    refreshError = null
                    state = UiState.Success(it)
                },
                onFailure = { error ->
                    if (!watch.still(started)) return@fold
                    if (previous is UiState.Success) {
                        refreshError = error.message
                        state = previous
                    } else {
                        state = UiState.Error(error.message ?: "Profile could not be loaded.")
                    }
                },
            )
        }
    }

    fun save(request: ProfileUpdateRequest) {
        if (saving) return
        saving = true
        saved = false
        saveError = null
        val started = watch.stamp()
        viewModelScope.launch {
            try {
                profileRepository.update(request).fold(
                    onSuccess = {
                        if (!watch.still(started)) return@fold
                        state = UiState.Success(it)
                        saved = true
                    },
                    onFailure = { error ->
                        if (watch.still(started)) saveError = error.message ?: "Profile could not be updated."
                    },
                )
            } finally {
                if (watch.still(started)) saving = false
            }
        }
    }

    /** Suspends until the previous session is cleared, so navigation can wait for it. */
    suspend fun logout() {
        logoutState = UiState.Loading
        authRepository.logout()
        logoutState = UiState.Success(Unit)
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        state = UiState.Idle
        logoutState = UiState.Idle
        saveError = null
        saved = false
        saving = false
        refreshError = null
    }
}
