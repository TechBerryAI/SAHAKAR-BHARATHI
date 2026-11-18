package com.example.sahakarbharati.features.registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.SessionWatch
import com.example.sahakarbharati.data.model.RegistrationResult
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.IdleSession
import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val authRepository: AuthRepository,
    session: SessionStore = IdleSession,
) : ViewModel() {
    var state by mutableStateOf<UiState<RegistrationResult>>(UiState.Idle)
        private set
    var verificationToken by mutableStateOf<String?>(null)
        private set
    /** Catalog key for a local OTP status. Null when [otpNote] holds a server message. */
    var otpNoteKey by mutableStateOf<String?>(null)
        private set
    var otpNote by mutableStateOf("")
        private set
    var otpBusy by mutableStateOf(false)
        private set
    var otpSent by mutableStateOf(false)
        private set
    var otpIsError by mutableStateOf(false)
        private set
    var accountExists by mutableStateOf(false)
        private set
    private var challengeId: String? = null
    private val watch = SessionWatch(
        session,
        viewModelScope,
        cancelWork = false,
        onChange = {
            clearOtp()
            if (state !is UiState.Loading) state = UiState.Idle
        },
    )

    fun clearOtp() {
        verificationToken = null
        challengeId = null
        otpSent = false
        otpNoteKey = null
        otpNote = ""
        otpIsError = false
        accountExists = false
    }

    fun showNote(message: String, error: Boolean) {
        otpNoteKey = null
        otpNote = message
        otpIsError = error
    }

    fun consumeError() {
        if (state is UiState.Error) state = UiState.Idle
    }

    fun sendOtp(mobile: String) {
        if (otpBusy) return
        viewModelScope.launch {
            otpBusy = true
            otpNoteKey = null
            otpNote = ""
            otpIsError = false
            accountExists = false
            val result = authRepository.sendOtp(mobile)
            otpBusy = false
            result.fold(
                onSuccess = {
                    challengeId = it
                    otpSent = true
                    verificationToken = null
                    otpIsError = false
                    otpNoteKey = "otpSent"
                    otpNote = ""
                },
                onFailure = {
                    val message = it.message.orEmpty()
                    accountExists = message.contains("already exists for this mobile", ignoreCase = true)
                    otpIsError = true
                    if (message.isBlank()) {
                        otpNoteKey = "otpSendFailed"
                        otpNote = ""
                    } else {
                        otpNoteKey = null
                        otpNote = message
                    }
                },
            )
        }
    }

    fun confirmOtp(code: String) {
        // Check the format locally so the participant sees a plain message instead of the
        // server's field-validation text. The server still validates the code.
        if (code.length != 6 || !code.all(Char::isDigit)) {
            otpIsError = true
            otpNoteKey = "otpRequired"
            otpNote = ""
            return
        }
        val id = challengeId
        if (otpBusy || id.isNullOrBlank()) {
            if (id.isNullOrBlank()) {
                otpIsError = true
                otpNoteKey = "otpSendFirst"
                otpNote = ""
            }
            return
        }
        viewModelScope.launch {
            otpBusy = true
            val result = authRepository.confirmOtp(id, code)
            otpBusy = false
            result.fold(
                onSuccess = {
                    verificationToken = it
                    otpIsError = false
                    otpNoteKey = "otpVerified"
                    otpNote = ""
                },
                onFailure = {
                    val message = it.message.orEmpty()
                    verificationToken = null
                    if (message.isBlank()) {
                        otpNoteKey = "otpVerifyFailed"
                        otpNote = ""
                    } else {
                        otpNoteKey = null
                        otpNote = message
                    }
                    // A locked code cannot be tried again; the website returns to Send OTP.
                    if (message.contains("Too many attempts", ignoreCase = true)) {
                        challengeId = null
                        otpSent = false
                    }
                    otpIsError = true
                },
            )
        }
    }

    fun submit(request: RegistrationRequest) {
        if (state is UiState.Loading) return
        // Set before launching so a second tap is rejected even if the coroutine has not started.
        state = UiState.Loading
        viewModelScope.launch {
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

    override fun onCleared() {
        watch.close()
    }
}
