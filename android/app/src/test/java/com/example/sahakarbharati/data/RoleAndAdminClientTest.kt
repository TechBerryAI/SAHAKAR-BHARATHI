package com.example.sahakarbharati.data

import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.knownRole
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.remote.AdminAttendanceListDto
import com.example.sahakarbharati.data.remote.AdminUserDetailDto
import com.example.sahakarbharati.data.remote.PlaceActiveRequest
import com.example.sahakarbharati.data.remote.PlaceCreateRequest
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.remote.PlaceListDto
import com.example.sahakarbharati.data.remote.AttendanceListDto
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.CheckInRequest
import com.example.sahakarbharati.data.remote.AdminUserListDto
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmRequest
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmedDto
import com.example.sahakarbharati.data.remote.PhoneVerificationCreatedDto
import com.example.sahakarbharati.data.remote.PhoneVerificationRequest
import com.example.sahakarbharati.data.remote.ProfileResponseDto
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.remote.RegistrationResponseDto
import com.example.sahakarbharati.data.remote.RoleUpdateRequest
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.SessionRequest
import com.example.sahakarbharati.data.remote.SessionResponseDto
import com.example.sahakarbharati.data.repository.ADMIN_PAGE_SIZE
import com.example.sahakarbharati.data.repository.RemoteAdminRepository
import com.example.sahakarbharati.data.repository.SessionStore
import com.example.sahakarbharati.data.repository.toProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAndAdminClientTest {

    @Test
    fun knownRole_keepsStaffAndFallsBackToParticipant() {
        assertEquals("admin", knownRole("admin"))
        assertEquals("super_admin", knownRole("super_admin"))
        assertEquals("participant", knownRole("participant"))
        assertEquals("participant", knownRole(null))
        assertEquals("participant", knownRole("owner"))
        assertTrue(isStaffRole("admin"))
        assertTrue(isStaffRole("super_admin"))
        assertFalse(isStaffRole("participant"))
        assertFalse(isStaffRole(null))
    }

    @Test
    fun profileDto_mapsUnknownRoleToParticipant() {
        val dto = ProfileResponseDto(
            loginId = "SB-1",
            fullName = "Asha",
            mobile = "9999999999",
            email = null,
            gender = null,
            dateOfBirth = null,
            occupation = null,
            addressLine = null,
            locality = null,
            district = null,
            state = null,
            pincode = null,
            status = "active",
            role = "owner",
        )
        assertEquals("participant", dto.toProfile().role)
        assertEquals("super_admin", dto.copy(role = "super_admin").toProfile().role)
    }

    @Test
    fun listUsers_sendsTrimmedQueryLimitAndOffset() = runTest {
        val api = RecordingAdminApi()
        val repository = RemoteAdminRepository(api, TokenSession("token-1"))

        repository.listUsers("  ravi ", 40)
        assertEquals("Bearer token-1", api.authorization)
        assertEquals("ravi", api.query)
        assertEquals(ADMIN_PAGE_SIZE, api.limit)
        assertEquals(40, api.offset)

        repository.listUsers("   ", 0)
        assertNull(api.query)
        assertEquals(0, api.offset)
    }
}

private class TokenSession(private val token: String) : SessionStore {
    override fun isLoggedIn(): Boolean = true
    override fun accessToken(): String = token
    override fun cachedProfile(): ParticipantProfile? = null
    override fun saveSession(token: String, profile: ParticipantProfile) = Unit
    override fun saveProfile(profile: ParticipantProfile) = Unit
    override fun clearSession() = Unit
}

private class RecordingAdminApi : SahakarApi {
    var authorization: String? = null
    var query: String? = "unset"
    var limit: Int = -1
    var offset: Int = -1

    override suspend fun listAdminUsers(
        authorization: String,
        query: String?,
        limit: Int,
        offset: Int,
    ): AdminUserListDto {
        this.authorization = authorization
        this.query = query
        this.limit = limit
        this.offset = offset
        return AdminUserListDto(emptyList(), 0, limit, offset)
    }

    override suspend fun sendPhoneVerification(body: PhoneVerificationRequest): PhoneVerificationCreatedDto = error("unused")
    override suspend fun confirmPhoneVerification(
        challengeId: String,
        body: PhoneVerificationConfirmRequest,
    ): PhoneVerificationConfirmedDto = error("unused")
    override suspend fun register(body: RegistrationRequest): RegistrationResponseDto = error("unused")
    override suspend fun createSession(body: SessionRequest): SessionResponseDto = error("unused")
    override suspend fun logout(authorization: String) = error("unused")
    override suspend fun me(authorization: String): ProfileResponseDto = error("unused")
    override suspend fun updateMe(authorization: String, body: ProfileUpdateRequest): ProfileResponseDto = error("unused")
    override suspend fun getAdminUser(authorization: String, userId: String): AdminUserDetailDto = error("unused")
    override suspend fun setAdminUserRole(
        authorization: String,
        userId: String,
        body: RoleUpdateRequest,
    ): AdminUserDetailDto = error("unused")
    override suspend fun checkIn(authorization: String, body: CheckInRequest): AttendanceRecordDto = error("unused")
    override suspend fun myAttendance(authorization: String): AttendanceListDto = error("unused")
    override suspend fun listPlaces(authorization: String): PlaceListDto = error("unused")
    override suspend fun getPlace(authorization: String, placeId: String): PlaceDto = error("unused")
    override suspend fun createPlace(authorization: String, body: PlaceCreateRequest): PlaceDto = error("unused")
    override suspend fun updatePlace(authorization: String, placeId: String, body: PlaceActiveRequest): PlaceDto = error("unused")
    override suspend fun listPlaceAttendance(
        authorization: String,
        placeId: String,
        limit: Int,
        offset: Int,
    ): AdminAttendanceListDto = error("unused")
}
