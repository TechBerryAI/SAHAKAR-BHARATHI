package com.example.sahakarbharati.features.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.repository.ProfileRepository
import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.launch

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    var state by mutableStateOf<UiState<ParticipantProfile>>(
        sessionStore.cachedProfile()?.let { UiState.Success(it) } ?: UiState.Idle,
    )
        private set

    fun load() {
        viewModelScope.launch {
            if (state !is UiState.Success) {
                state = UiState.Loading
            }
            state = profileRepository.me().fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { error ->
                    val cached = sessionStore.cachedProfile()
                    if (cached != null) {
                        UiState.Success(cached)
                    } else {
                        UiState.Error(error.message ?: "Profile could not be loaded.")
                    }
                },
            )
        }
    }
}
