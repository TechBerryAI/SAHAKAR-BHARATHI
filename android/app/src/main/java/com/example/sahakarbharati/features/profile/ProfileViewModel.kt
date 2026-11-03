package com.example.sahakarbharati.features.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
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

    fun load() {
        viewModelScope.launch {
            val previous = state
            if (previous !is UiState.Success) {
                state = UiState.Loading
            }
            profileRepository.me().fold(
                onSuccess = {
                    refreshError = null
                    state = UiState.Success(it)
                },
                onFailure = { error ->
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
        viewModelScope.launch {
            saving = true
            saved = false
            saveError = null
            profileRepository.update(request).fold(
                onSuccess = {
                    state = UiState.Success(it)
                    saved = true
                },
                onFailure = { saveError = it.message ?: "Profile could not be updated." },
            )
            saving = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutState = UiState.Loading
            authRepository.logout()
            logoutState = UiState.Success(Unit)
        }
    }
}
