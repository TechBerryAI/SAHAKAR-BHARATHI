package com.example.sahakarbharati.data.model

data class AdminUserSummary(
    val id: String,
    val loginId: String,
    val fullName: String,
    val mobile: String,
    val district: String?,
    val state: String?,
    val role: String,
    val status: String,
)

data class AdminUserPage(
    val items: List<AdminUserSummary>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

data class AdminUserDetail(
    val id: String,
    val createdAt: String?,
    val profile: ParticipantProfile,
)
