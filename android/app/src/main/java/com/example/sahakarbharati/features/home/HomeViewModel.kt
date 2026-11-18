package com.example.sahakarbharati.features.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.SessionWatch
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

    private val watch = SessionWatch(sessionStore, viewModelScope, onChange = ::clearAccount)

    fun load() {
        val started = watch.stamp()
        viewModelScope.launch {
            if (state !is UiState.Success) {
                state = UiState.Loading
            }
            val next = profileRepository.me().fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { error ->
                    // Offline or server trouble: keep showing the cached profile. A rejected
                    // session has already been cleared, so there is nothing cached to show.
                    val cached = sessionStore.cachedProfile()?.takeIf { sessionStore.isLoggedIn() }
                    if (cached != null) {
                        UiState.Success(cached)
                    } else {
                        UiState.Error(error.message ?: "Profile could not be loaded.")
                    }
                },
            )
            if (watch.still(started)) state = next
        }
    }

    override fun onCleared() {
        watch.close()
    }

    private fun clearAccount() {
        val cached = sessionStore.cachedProfile()?.takeIf { sessionStore.isLoggedIn() }
        state = if (cached != null) UiState.Success(cached) else UiState.Idle
    }
}
