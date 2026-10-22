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
    var verificationToken by mutableStateOf<String?>(null)
        private set
    var otpNote by mutableStateOf("")
        private set
    var otpBusy by mutableStateOf(false)
        private set
    private var challengeId: String? = null

    fun clearOtp() {
        verificationToken = null
        challengeId = null
        otpNote = ""
    }

    fun sendOtp(mobile: String) {
        if (otpBusy) return
        viewModelScope.launch {
            otpBusy = true
            otpNote = ""
            val result = authRepository.sendOtp(mobile)
            otpBusy = false
            result.fold(
                onSuccess = {
                    challengeId = it
                    verificationToken = null
                    otpNote = "OTP sent. It expires in 5 minutes."
                },
                onFailure = { otpNote = it.message ?: "Could not send the OTP." },
            )
        }
    }

    fun confirmOtp(code: String) {
        val id = challengeId
        if (otpBusy || id.isNullOrBlank()) {
            if (id.isNullOrBlank()) otpNote = "Send an OTP before verifying."
            return
        }
        viewModelScope.launch {
            otpBusy = true
            val result = authRepository.confirmOtp(id, code)
            otpBusy = false
            result.fold(
                onSuccess = {
                    verificationToken = it
                    otpNote = "Mobile number verified."
                },
                onFailure = {
                    verificationToken = null
                    otpNote = it.message ?: "Could not verify the OTP."
                },
            )
        }
    }

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
