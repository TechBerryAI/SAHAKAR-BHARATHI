package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.remote.AdminAttendanceListDto
import com.example.sahakarbharati.data.remote.AdminUserDetailDto
import com.example.sahakarbharati.data.remote.AdminUserListDto
import com.example.sahakarbharati.data.remote.AttendanceListDto
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.BoothCodeDto
import com.example.sahakarbharati.data.remote.BoothCodeListDto
import com.example.sahakarbharati.data.remote.CheckInRequest
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmRequest
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmedDto
import com.example.sahakarbharati.data.remote.PhoneVerificationCreatedDto
import com.example.sahakarbharati.data.remote.PhoneVerificationRequest
import com.example.sahakarbharati.data.remote.PlaceActiveRequest
import com.example.sahakarbharati.data.remote.PlaceCreateRequest
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.remote.PlaceListDto
import com.example.sahakarbharati.data.remote.ProfileResponseDto
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.remote.RegistrationResponseDto
import com.example.sahakarbharati.data.remote.RoleUpdateRequest
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.SessionRequest
import com.example.sahakarbharati.data.remote.SessionResponseDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteRepositoryCoverageTest {
    @Test
    fun authOtpSignInAndLogoutUseTheSessionInOrder() = runTest {
        val api = RecordingApi()
        val session = OrderSession("token-1")
        val auth = RemoteAuthRepository(api, session)

        assertEquals("challenge-1", auth.sendOtp("9876543210").getOrThrow())
        assertEquals("9876543210", api.mobile)
        assertEquals("verify-1", auth.confirmOtp("challenge-1", "123456").getOrThrow())

        val signedIn = auth.signIn("9876543210", "2468").getOrThrow()
        assertEquals("token-1", signedIn.accessToken)
        assertEquals("anita@example.com", signedIn.profile.email)
        assertEquals("admin", signedIn.profile.role)
        assertEquals("token-1", session.token)
        assertEquals("Anita Desai", session.profile?.fullName)

        auth.logout()
        assertEquals(listOf("clear", "logout"), session.events + api.calls.filter { it == "logout" })
        assertNull(session.token)

        val signedOut = RemoteAuthRepository(api, OrderSession(null))
        signedOut.logout()
        assertEquals(1, api.calls.count { it == "logout" })
    }

    @Test
    fun registerFailureDoesNotClaimTheAccountWasCreated() = runTest {
        val api = RecordingApi().apply { failRegister = true }
        val result = RemoteAuthRepository(api, OrderSession(null)).register(registration())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.startsWith("Registration failed").not())
        assertEquals("Mobile number or PIN is incorrect.", result.exceptionOrNull()!!.message)
    }

    @Test
    fun profileAndAttendanceRequireATokenAndSaveTheLoadedProfile() = runTest {
        val api = RecordingApi()
        val session = OrderSession("token-1")
        val profile = RemoteProfileRepository(api, session)
        assertEquals("Anita Desai", profile.me().getOrThrow().fullName)
        assertEquals("Bearer token-1", api.authorization)
        assertEquals("Anita Desai", session.profile?.fullName)

        val updated = profile.update(ProfileUpdateRequest(fullName = "Anita Updated")).getOrThrow()
        assertEquals("Anita Updated", updated.fullName)
        assertEquals("Anita Updated", session.profile?.fullName)

        val attendance = RemoteAttendanceRepository(api, session)
        assertEquals("004218", attendance.checkIn("004218", "code").getOrThrow().placeName.let { api.code })
        assertEquals("code", api.source)
        assertEquals(1, attendance.mine().getOrThrow().total)
        assertEquals("Gate", attendance.boothCodes().getOrThrow().single().name)

        val signedOut = RemoteAttendanceRepository(api, OrderSession(null))
        assertEquals("Sign in is required.", signedOut.checkIn("1", "qr").exceptionOrNull()?.message)
    }

    @Test
    fun adminRejectsAnUnknownRoleAndTrimsABoothNameBeforeTheRequest() = runTest {
        val api = RecordingApi()
        val admin = RemoteAdminRepository(api, OrderSession("token-1"))
        val rejected = admin.setRole("user-1", "owner")
        assertTrue(rejected.isFailure)
        assertTrue(api.calls.none { it == "role" })

        assertEquals("admin", admin.setRole("user-1", "admin").getOrThrow().profile.role)
        assertEquals("admin", api.role)
        assertEquals("Ravi", admin.getUser("user-1").getOrThrow().profile.fullName)
        assertEquals("Gate booth", admin.createPlace("  Gate booth  ").getOrThrow().name)
        assertEquals("Gate booth", api.placeName)
        assertEquals(false, admin.setPlaceActive("place-1", false).getOrThrow().active)
        admin.listPlaceAttendance("place-1", 0)
        assertEquals(50, api.attendanceLimit)
        assertEquals(1, admin.listPlaces().getOrThrow().size)
    }

    @Test
    fun registrationAndSessionPayloadsMapOntoProfiles() {
        val registered = RegistrationResponseDto(
            registrationId = "SB-1",
            loginId = "SB-1",
            fullName = "Anita Desai",
            mobile = "9876543210",
            occupation = "Farmer",
            state = "Maharashtra",
            district = "Nashik",
        ).toProfile()
        assertEquals("Nashik", registered.district)
        assertEquals("Farmer", registered.occupation)

        val session = SessionResponseDto(
            accessToken = "token-1",
            tokenType = "bearer",
            expiresAt = null,
            loginId = "SB-1",
            fullName = "Anita Desai",
            mobile = "9876543210",
            occupation = "Farmer",
            state = "Maharashtra",
            district = "Nashik",
            email = "anita@example.com",
            gender = "Female",
            dateOfBirth = "1985-03-12",
            pincode = "422001",
            status = "active",
            role = "super_admin",
        ).toResult()
        assertEquals("super_admin", session.profile.role)
        assertEquals("422001", session.profile.pincode)
        assertEquals("1985-03-12", session.profile.dateOfBirth)
    }

    private fun registration() = RegistrationRequest(
        fullName = "Anita Desai",
        gender = null,
        dateOfBirth = null,
        occupation = null,
        mobile = "9876543210",
        email = null,
        addressLine = null,
        locality = null,
        district = null,
        state = null,
        pincode = null,
        pin = "2468",
        verificationToken = "token",
    )
}

private class OrderSession(var token: String?) : SessionStore {
    val events = mutableListOf<String>()
    var profile: ParticipantProfile? = null
    override fun isLoggedIn(): Boolean = !token.isNullOrBlank()
    override fun accessToken(): String? = token
    override fun cachedProfile(): ParticipantProfile? = profile
    override fun saveSession(token: String, profile: ParticipantProfile) {
        this.token = token
        this.profile = profile
    }
    override fun saveProfile(profile: ParticipantProfile) {
        this.profile = profile
    }
    override fun clearSession() {
        events += "clear"
        token = null
        profile = null
    }
}

private class RecordingApi : SahakarApi {
    val calls = mutableListOf<String>()
    var authorization: String? = null
    var mobile: String? = null
    var placeName: String? = null
    var role: String? = null
    var code: String? = null
    var source: String? = null
    var attendanceLimit: Int = -1
    var failRegister = false

    override suspend fun sendPhoneVerification(body: PhoneVerificationRequest): PhoneVerificationCreatedDto {
        mobile = body.mobile
        return PhoneVerificationCreatedDto("challenge-1", 300)
    }

    override suspend fun confirmPhoneVerification(
        challengeId: String,
        body: PhoneVerificationConfirmRequest,
    ) = PhoneVerificationConfirmedDto("verify-1", 900)

    override suspend fun register(body: RegistrationRequest): RegistrationResponseDto {
        if (failRegister) {
            throw HttpException(
                Response.error<Any>(
                    401,
                    """{"detail":"Mobile number or PIN is incorrect."}""".toResponseBody("application/json".toMediaType()),
                ),
            )
        }
        return RegistrationResponseDto("SB-1", "SB-1", body.fullName, body.mobile, null, null, null)
    }

    override suspend fun createSession(body: SessionRequest) = SessionResponseDto(
        accessToken = "token-1",
        tokenType = "bearer",
        expiresAt = null,
        loginId = "SB-1",
        fullName = "Anita Desai",
        mobile = body.identifier,
        occupation = "Farmer",
        state = "Maharashtra",
        district = "Nashik",
        email = "anita@example.com",
        role = "admin",
    )

    override suspend fun logout(authorization: String) {
        calls += "logout"
    }

    override suspend fun me(authorization: String): ProfileResponseDto {
        this.authorization = authorization
        return profile("Anita Desai")
    }

    override suspend fun updateMe(authorization: String, body: ProfileUpdateRequest) = profile(body.fullName ?: "Anita Desai")

    override suspend fun listAdminUsers(authorization: String, query: String?, limit: Int, offset: Int) =
        AdminUserListDto(emptyList(), 0, limit, offset)

    override suspend fun getAdminUser(authorization: String, userId: String) = detail("Ravi", "participant")

    override suspend fun setAdminUserRole(authorization: String, userId: String, body: RoleUpdateRequest): AdminUserDetailDto {
        calls += "role"
        role = body.role
        return detail("Ravi", body.role)
    }

    override suspend fun checkIn(authorization: String, body: CheckInRequest): AttendanceRecordDto {
        code = body.code
        source = body.source
        return visit()
    }

    override suspend fun myAttendance(authorization: String) = AttendanceListDto(listOf(visit()), 1)

    override suspend fun listBoothCodes(authorization: String) = BoothCodeListDto(listOf(BoothCodeDto("Gate", "004218")))

    override suspend fun listPlaces(authorization: String) = PlaceListDto(listOf(place(true)))

    override suspend fun getPlace(authorization: String, placeId: String) = place(true)

    override suspend fun createPlace(authorization: String, body: PlaceCreateRequest): PlaceDto {
        placeName = body.name
        return place(true)
    }

    override suspend fun updatePlace(authorization: String, placeId: String, body: PlaceActiveRequest) = place(body.active)

    override suspend fun listPlaceAttendance(
        authorization: String,
        placeId: String,
        limit: Int,
        offset: Int,
    ): AdminAttendanceListDto {
        attendanceLimit = limit
        return AdminAttendanceListDto(emptyList(), 0, limit, offset)
    }

    private fun profile(name: String) = ProfileResponseDto(
        loginId = "SB-1",
        fullName = name,
        mobile = "9876543210",
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
        role = "participant",
    )

    private fun detail(name: String, role: String) = AdminUserDetailDto(
        id = "user-1",
        loginId = "SB-1",
        fullName = name,
        mobile = "9876543210",
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
        role = role,
    )

    private fun visit() = AttendanceRecordDto(
        id = "visit-1",
        placeId = "place-1",
        placeName = "Gate",
        checkedInAt = "2026-11-03T10:13:00Z",
        attendanceDate = "2026-11-03",
        source = "code",
        loginId = "SB-1",
        fullName = "Anita Desai",
        mobile = "9876543210",
        email = null,
        gender = null,
        occupation = null,
        addressLine = null,
        locality = null,
        district = null,
        state = null,
        pincode = null,
    )

    private fun place(active: Boolean) = PlaceDto("place-1", "Gate booth", "004218", active, "2026-11-03T00:00:00Z")
}
