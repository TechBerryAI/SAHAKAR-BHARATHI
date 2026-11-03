package com.example.sahakarbharati.data.model

const val ROLE_PARTICIPANT = "participant"
const val ROLE_ADMIN = "admin"
const val ROLE_SUPER_ADMIN = "super_admin"

/** Matches the website: only the three known roles are kept, anything else is a participant. */
fun knownRole(value: String?): String = when (value) {
    ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_PARTICIPANT -> value
    else -> ROLE_PARTICIPANT
}

fun isStaffRole(role: String?): Boolean = role == ROLE_ADMIN || role == ROLE_SUPER_ADMIN

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
    val role: String = ROLE_PARTICIPANT,
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
