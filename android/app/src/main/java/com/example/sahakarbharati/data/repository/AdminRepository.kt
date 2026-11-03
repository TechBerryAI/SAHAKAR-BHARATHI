package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserPage
import com.example.sahakarbharati.data.model.AdminUserSummary
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.ROLE_ADMIN
import com.example.sahakarbharati.data.model.ROLE_PARTICIPANT
import com.example.sahakarbharati.data.model.knownRole
import com.example.sahakarbharati.data.remote.AdminUserDetailDto
import com.example.sahakarbharati.data.remote.AdminUserListDto
import com.example.sahakarbharati.data.remote.AdminUserSummaryDto
import com.example.sahakarbharati.data.remote.RoleUpdateRequest
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.mapApiError

const val ADMIN_PAGE_SIZE = 20

interface AdminRepository {
    suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage>
    suspend fun getUser(id: String): Result<AdminUserDetail>
    suspend fun setRole(id: String, role: String): Result<AdminUserDetail>
}

class RemoteAdminRepository(
    private val api: SahakarApi,
    private val sessionStore: SessionStore,
) : AdminRepository {
    override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        val q = query.trim().ifBlank { null }
        return runCatching {
            api.listAdminUsers("Bearer $token", q, ADMIN_PAGE_SIZE, offset).toPage()
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not load registered users."))
        }
    }

    override suspend fun getUser(id: String): Result<AdminUserDetail> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.getAdminUser("Bearer $token", id).toDetail()
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not load this person."))
        }
    }

    override suspend fun setRole(id: String, role: String): Result<AdminUserDetail> {
        if (role != ROLE_ADMIN && role != ROLE_PARTICIPANT) {
            return Result.failure(Exception("Could not update access."))
        }
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.setAdminUserRole("Bearer $token", id, RoleUpdateRequest(role)).toDetail()
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not update access."))
        }
    }
}

private fun AdminUserListDto.toPage() = AdminUserPage(
    items = items.map { it.toSummary() },
    total = total,
    limit = limit,
    offset = offset,
)

private fun AdminUserSummaryDto.toSummary() = AdminUserSummary(
    id = id,
    loginId = loginId,
    fullName = fullName,
    mobile = mobile,
    district = district,
    state = state,
    role = knownRole(role),
    status = status ?: "active",
)

private fun AdminUserDetailDto.toDetail() = AdminUserDetail(
    id = id,
    createdAt = createdAt,
    profile = ParticipantProfile(
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
        role = knownRole(role),
    ),
)
