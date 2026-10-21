package com.example.sahakarbharati.data.model

data class ParticipantProfile(
    val loginId: String,
    val fullName: String,
    val mobile: String,
    val email: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val occupation: String? = null,
    val addressLine: String? = null,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val status: String? = null,
) {
    val firstName: String
        get() = fullName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()

    val region: String
        get() = listOfNotNull(district?.takeIf { it.isNotBlank() }, state?.takeIf { it.isNotBlank() })
            .joinToString(" • ")
}

data class RegistrationResult(
    val registrationId: String,
    val profile: ParticipantProfile,
)

data class SessionResult(
    val accessToken: String,
    val profile: ParticipantProfile,
)
