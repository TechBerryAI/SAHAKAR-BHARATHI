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

    fun load() {
        viewModelScope.launch {
            state = UiState.Loading
            state = profileRepository.me().fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Profile could not be loaded.") },
            )
        }
    }

    fun save(request: ProfileUpdateRequest) {
        viewModelScope.launch {
            state = UiState.Loading
            state = profileRepository.update(request).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Profile could not be updated.") },
            )
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
