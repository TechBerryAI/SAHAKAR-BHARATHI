package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserPage
import com.example.sahakarbharati.data.model.AdminUserSummary
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.ROLE_ADMIN
import com.example.sahakarbharati.data.model.ROLE_PARTICIPANT
import com.example.sahakarbharati.data.model.knownRole
import com.example.sahakarbharati.data.remote.AdminAttendanceListDto
import com.example.sahakarbharati.data.remote.AdminUserDetailDto
import com.example.sahakarbharati.data.remote.AdminUserListDto
import com.example.sahakarbharati.data.remote.AdminUserSummaryDto
import com.example.sahakarbharati.data.remote.PlaceActiveRequest
import com.example.sahakarbharati.data.remote.PlaceCreateRequest
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.remote.RoleUpdateRequest
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.mapApiError

const val ADMIN_PAGE_SIZE = 20
const val PLACE_ATTENDANCE_PAGE_SIZE = 50

interface AdminRepository {
    suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage>
    suspend fun getUser(id: String): Result<AdminUserDetail>
    suspend fun setRole(id: String, role: String): Result<AdminUserDetail>
    suspend fun listPlaces(): Result<List<PlaceDto>>
    suspend fun getPlace(id: String): Result<PlaceDto>
    suspend fun createPlace(name: String): Result<PlaceDto>
    suspend fun setPlaceActive(id: String, active: Boolean): Result<PlaceDto>
    suspend fun listPlaceAttendance(id: String, offset: Int): Result<AdminAttendanceListDto>
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

    override suspend fun listPlaces(): Result<List<PlaceDto>> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.listPlaces("Bearer $token").items
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not load booths."))
        }
    }

    override suspend fun getPlace(id: String): Result<PlaceDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.getPlace("Bearer $token", id)
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not load this booth."))
        }
    }

    override suspend fun createPlace(name: String): Result<PlaceDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.createPlace("Bearer $token", PlaceCreateRequest(name.trim()))
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not create this booth."))
        }
    }

    override suspend fun setPlaceActive(id: String, active: Boolean): Result<PlaceDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.updatePlace("Bearer $token", id, PlaceActiveRequest(active))
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not update this booth."))
        }
    }

    override suspend fun listPlaceAttendance(id: String, offset: Int): Result<AdminAttendanceListDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.listPlaceAttendance("Bearer $token", id, PLACE_ATTENDANCE_PAGE_SIZE, offset)
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Could not load attendance."))
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
