package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.mapApiError

interface ProfileRepository {
    suspend fun me(): Result<ParticipantProfile>
    suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile>
}

class RemoteProfileRepository(
    private val api: SahakarApi,
    private val sessionStore: SessionStore,
) : ProfileRepository {
    override suspend fun me(): Result<ParticipantProfile> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            val profile = api.me("Bearer $token").toProfile()
            sessionStore.saveProfile(profile)
            profile
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Profile could not be loaded."))
        }
    }

    override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            val profile = api.updateMe("Bearer $token", request).toProfile()
            sessionStore.saveProfile(profile)
            profile
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Profile could not be updated."))
        }
    }
}
