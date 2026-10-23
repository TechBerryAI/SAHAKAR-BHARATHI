package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.RegistrationResult
import com.example.sahakarbharati.data.model.SessionResult
import com.example.sahakarbharati.data.remote.ProfileResponseDto
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmRequest
import com.example.sahakarbharati.data.remote.PhoneVerificationRequest
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.remote.RegistrationResponseDto
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.SessionRequest
import com.example.sahakarbharati.data.remote.SessionResponseDto
import com.example.sahakarbharati.data.remote.mapApiError

interface AuthRepository {
    suspend fun sendOtp(mobile: String): Result<String>
    suspend fun confirmOtp(challengeId: String, code: String): Result<String>
    suspend fun register(request: RegistrationRequest): Result<RegistrationResult>
    suspend fun signIn(identifier: String, pin: String): Result<SessionResult>
    suspend fun logout()
}

class RemoteAuthRepository(
    private val api: SahakarApi,
    private val sessionStore: SessionStore,
) : AuthRepository {
    override suspend fun sendOtp(mobile: String): Result<String> {
        return runCatching {
            api.sendPhoneVerification(PhoneVerificationRequest(mobile)).id
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not send the OTP."))
        }
    }

    override suspend fun confirmOtp(challengeId: String, code: String): Result<String> {
        return runCatching {
            api.confirmPhoneVerification(challengeId, PhoneVerificationConfirmRequest(code)).verificationToken
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not verify the OTP."))
        }
    }

    override suspend fun register(request: RegistrationRequest): Result<RegistrationResult> {
        val created = runCatching { api.register(request) }.getOrElse { error ->
            return Result.failure(Exception(mapApiError(error, "Registration failed.")))
        }
        // The account now exists. If signing in fails, say so instead of "Registration failed",
        // which would send the participant into a duplicate-account retry.
        return runCatching {
            val result = api.createSession(SessionRequest(request.mobile, request.pin)).toResult()
            sessionStore.saveSession(result.accessToken, result.profile)
            RegistrationResult(created.registrationId, result.profile.withRegistration(created))
        }.recoverCatching { error ->
            throw Exception(
                "Your account was created (Login ID ${created.loginId}), but signing in failed: " +
                    mapApiError(error, "please try again.") + " Sign in with your Login ID and PIN.",
            )
        }
    }

    override suspend fun signIn(identifier: String, pin: String): Result<SessionResult> {
        return runCatching {
            val session = api.createSession(SessionRequest(identifier, pin)).toResult()
            sessionStore.saveSession(session.accessToken, session.profile)
            session
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Sign in failed."))
        }
    }

    override suspend fun logout() {
        val token = sessionStore.accessToken()
        sessionStore.clearSession()
        if (!token.isNullOrBlank()) {
            runCatching { api.logout("Bearer $token") }
        }
    }
}

fun RegistrationResponseDto.toProfile(): ParticipantProfile =
    ParticipantProfile(
        loginId = loginId,
        fullName = fullName,
        mobile = mobile,
        occupation = occupation,
        district = district,
        state = state,
    )

fun SessionResponseDto.toResult(): SessionResult =
    SessionResult(
        accessToken = accessToken,
        profile = ParticipantProfile(
            loginId = loginId,
            fullName = fullName,
            mobile = mobile,
            occupation = occupation,
            district = district,
            state = state,
        ),
    )

fun ProfileResponseDto.toProfile(): ParticipantProfile =
    ParticipantProfile(
        loginId = loginId,
        fullName = fullName,
        mobile = mobile,
        email = email,
        gender = gender,
        dateOfBirth = dateOfBirth,
        occupation = occupation,
        addressLine = addressLine,
        locality = locality,
        district = district,
        state = state,
        pincode = pincode,
        status = status,
    )

private fun ParticipantProfile.withRegistration(created: RegistrationResponseDto): ParticipantProfile =
    copy(
        loginId = created.loginId,
        fullName = created.fullName,
        mobile = created.mobile,
        occupation = created.occupation ?: occupation,
        district = created.district ?: district,
        state = created.state ?: state,
    )
