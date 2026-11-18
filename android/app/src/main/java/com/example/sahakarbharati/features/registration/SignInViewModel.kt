package com.example.sahakarbharati.features.registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.SessionWatch
import com.example.sahakarbharati.data.model.SessionResult
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.IdleSession
import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var state by mutableStateOf<UiState<SessionResult>>(UiState.Idle)
        private set

    private val watch = SessionWatch(
        session,
        viewModelScope,
        cancelWork = false,
        onChange = { if (state !is UiState.Loading) state = UiState.Idle },
    )

    fun submit(identifier: String, pin: String) {
        if (state is UiState.Loading) return
        // Set before launching so a second tap is rejected even if the coroutine has not started.
        state = UiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(identifier, pin)
            state = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Sign in failed.") },
            )
        }
    }

    fun consumeSuccess() {
        if (state is UiState.Success) {
            state = UiState.Idle
        }
    }

    override fun onCleared() {
        watch.close()
    }
}
