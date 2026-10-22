package com.example.sahakarbharati.features.registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.model.RegistrationResult
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var state by mutableStateOf<UiState<RegistrationResult>>(UiState.Idle)
        private set

    fun submit(request: RegistrationRequest) {
        if (state is UiState.Loading) return
        viewModelScope.launch {
            state = UiState.Loading
            val result = authRepository.register(request)
            state = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Registration failed.") },
            )
        }
    }

    fun consumeSuccess() {
        if (state is UiState.Success) {
            state = UiState.Idle
        }
    }
}
