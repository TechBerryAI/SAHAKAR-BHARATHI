package com.example.sahakarbharati.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface SahakarApi {
    @POST("api/v1/registrations")
    suspend fun register(@Body body: RegistrationRequest): RegistrationResponseDto

    @POST("api/v1/sessions")
    suspend fun createSession(@Body body: SessionRequest): SessionResponseDto

    @DELETE("api/v1/sessions/current")
    suspend fun logout(@Header("Authorization") authorization: String)

    @GET("api/v1/me")
    suspend fun me(@Header("Authorization") authorization: String): ProfileResponseDto

    @PATCH("api/v1/me")
    suspend fun updateMe(
        @Header("Authorization") authorization: String,
        @Body body: ProfileUpdateRequest,
    ): ProfileResponseDto
}
