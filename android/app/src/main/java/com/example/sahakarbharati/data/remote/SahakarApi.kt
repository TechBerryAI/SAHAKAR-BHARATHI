package com.example.sahakarbharati.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SahakarApi {
    @POST("api/v1/phone-verifications")
    suspend fun sendPhoneVerification(@Body body: PhoneVerificationRequest): PhoneVerificationCreatedDto

    @POST("api/v1/phone-verifications/{challengeId}/confirm")
    suspend fun confirmPhoneVerification(
        @Path("challengeId") challengeId: String,
        @Body body: PhoneVerificationConfirmRequest,
    ): PhoneVerificationConfirmedDto

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

    @GET("api/v1/admin/users")
    suspend fun listAdminUsers(
        @Header("Authorization") authorization: String,
        @Query("q") query: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): AdminUserListDto

    @GET("api/v1/admin/users/{userId}")
    suspend fun getAdminUser(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String,
    ): AdminUserDetailDto

    @POST("api/v1/admin/users/{userId}/role")
    suspend fun setAdminUserRole(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String,
        @Body body: RoleUpdateRequest,
    ): AdminUserDetailDto
}
