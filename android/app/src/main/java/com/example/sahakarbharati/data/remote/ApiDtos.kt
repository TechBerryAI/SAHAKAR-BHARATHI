package com.example.sahakarbharati.data.remote

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    @SerializedName("full_name") val fullName: String,
    val gender: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    val occupation: String?,
    val mobile: String,
    val email: String?,
    @SerializedName("address_line") val addressLine: String?,
    val locality: String?,
    val district: String?,
    val state: String?,
    val pincode: String?,
    val pin: String,
    @SerializedName("verification_token") val verificationToken: String,
)

data class PhoneVerificationRequest(
    val mobile: String,
)

data class PhoneVerificationCreatedDto(
    val id: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int,
)

data class PhoneVerificationConfirmRequest(
    val code: String,
)

data class PhoneVerificationConfirmedDto(
    @SerializedName("verification_token") val verificationToken: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int,
)

data class RegistrationResponseDto(
    @SerializedName("registration_id") val registrationId: String,
    @SerializedName("login_id") val loginId: String,
    @SerializedName("full_name") val fullName: String,
    val mobile: String,
    val occupation: String?,
    val state: String?,
    val district: String?,
)

data class SessionRequest(
    val identifier: String,
    val pin: String,
)

data class SessionResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("login_id") val loginId: String,
    @SerializedName("full_name") val fullName: String,
    val mobile: String,
    val occupation: String?,
    val state: String?,
    val district: String?,
)

data class ProfileResponseDto(
    @SerializedName("login_id") val loginId: String,
    @SerializedName("full_name") val fullName: String,
    val mobile: String,
    val email: String?,
    val gender: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    val occupation: String?,
    @SerializedName("address_line") val addressLine: String?,
    val locality: String?,
    val district: String?,
    val state: String?,
    val pincode: String?,
    val status: String?,
)

data class ProfileUpdateRequest(
    @SerializedName("full_name") val fullName: String? = null,
    val email: String? = null,
    val occupation: String? = null,
    @SerializedName("address_line") val addressLine: String? = null,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val pincode: String? = null,
)
