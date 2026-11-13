package com.example.sahakarbharati.features

import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserPage
import com.example.sahakarbharati.data.model.AdminUserSummary
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.RegistrationResult
import com.example.sahakarbharati.data.remote.AdminAttendanceListDto
import com.example.sahakarbharati.data.remote.AttendanceListDto
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.BoothCodeDto
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.repository.AdminRepository
import com.example.sahakarbharati.data.repository.AttendanceRepository
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.ProfileRepository
import com.example.sahakarbharati.data.repository.SessionStore
import com.example.sahakarbharati.features.admin.AdminPlaceViewModel
import com.example.sahakarbharati.features.admin.AdminPlacesViewModel
import com.example.sahakarbharati.features.admin.AdminUserViewModel
import com.example.sahakarbharati.features.admin.AdminUsersViewModel
import com.example.sahakarbharati.features.attendance.AttendanceViewModel
import com.example.sahakarbharati.features.home.HomeViewModel
import com.example.sahakarbharati.features.profile.ProfileViewModel
import com.example.sahakarbharati.features.registration.RegistrationViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun attendanceCheckInIgnoresASecondTapAndDropsADuplicateVisit() = runTest {
        val gate = CompletableDeferred<Result<AttendanceRecordDto>>()
        var calls = 0
        val repository = object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> {
                calls += 1
                return gate.await()
            }

            override suspend fun mine(): Result<AttendanceListDto> =
                Result.success(AttendanceListDto(listOf(visit("visit-1")), 1))

            override suspend fun boothCodes(): Result<List<BoothCodeDto>> = Result.success(emptyList())
        }
        val viewModel = AttendanceViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        viewModel.checkIn("004218", "qr")
        runCurrent()
        viewModel.checkIn("004218", "code")
        runCurrent()
        assertEquals(1, calls)

        gate.complete(Result.success(visit("visit-1")))
        advanceUntilIdle()
        assertEquals(listOf("visit-1"), viewModel.state.history.map { it.id })
        assertEquals("visit-1", viewModel.state.latest?.id)
        assertFalse(viewModel.state.submitting)
    }

    @Test
    fun profileKeepsThePreviousProfileAndLogoutClearsThroughAuth() = runTest {
        var me: Result<ParticipantProfile> = Result.success(profile("Anita Desai"))
        var loggedOut = false
        val profiles = object : ProfileRepository {
            override suspend fun me(): Result<ParticipantProfile> = me
            override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> =
                Result.success(profile(request.fullName ?: "Anita Desai"))
        }
        val auth = object : AuthRepository by unusedAuth() {
            override suspend fun logout() {
                loggedOut = true
            }
        }
        val viewModel = ProfileViewModel(profiles, auth)
        viewModel.load()
        advanceUntilIdle()
        assertTrue(viewModel.state is UiState.Success)

        me = Result.failure(Exception("offline"))
        viewModel.load()
        advanceUntilIdle()
        assertEquals("Anita Desai", (viewModel.state as UiState.Success).data.fullName)
        assertEquals("offline", viewModel.refreshError)

        viewModel.save(ProfileUpdateRequest(fullName = "Anita Updated"))
        advanceUntilIdle()
        assertEquals("Anita Updated", (viewModel.state as UiState.Success).data.fullName)
        assertTrue(viewModel.saved)

        viewModel.logout()
        advanceUntilIdle()
        assertTrue(loggedOut)
        assertTrue(viewModel.logoutState is UiState.Success)
    }

    @Test
    fun homeRefreshReplacesTheCachedProfile() = runTest {
        val session = MemorySession(profile("Cached"))
        val viewModel = HomeViewModel(
            object : ProfileRepository {
                override suspend fun me(): Result<ParticipantProfile> = Result.success(profile("Anita Desai"))
                override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> =
                    Result.failure(Exception("unused"))
            },
            session,
        )
        assertEquals("Cached", (viewModel.state as UiState.Success).data.fullName)
        viewModel.load()
        advanceUntilIdle()
        assertEquals("Anita Desai", (viewModel.state as UiState.Success).data.fullName)
    }

    @Test
    fun otpEdgesCoverAMissingChallengeABusyConfirmAndTooManyAttempts() = runTest {
        val gate = CompletableDeferred<Result<String>>()
        var confirms = 0
        val auth = object : AuthRepository by unusedAuth() {
            override suspend fun sendOtp(mobile: String): Result<String> = Result.success("challenge-1")
            override suspend fun confirmOtp(challengeId: String, code: String): Result<String> {
                confirms += 1
                return gate.await()
            }
            override suspend fun register(request: RegistrationRequest): Result<RegistrationResult> =
                Result.failure(Exception("nope"))
        }
        val viewModel = RegistrationViewModel(auth)
        viewModel.confirmOtp("123456")
        assertEquals("Send an OTP before verifying.", viewModel.otpNote)

        viewModel.sendOtp("9876543210")
        advanceUntilIdle()
        viewModel.confirmOtp("123456")
        runCurrent()
        viewModel.confirmOtp("654321")
        runCurrent()
        assertEquals(1, confirms)
        assertTrue(viewModel.otpBusy)

        gate.complete(Result.failure(Exception("Too many attempts. Try again later.")))
        advanceUntilIdle()
        assertFalse(viewModel.otpSent)
        assertNull(viewModel.verificationToken)
        assertEquals("Too many attempts. Try again later.", viewModel.otpNote)

        viewModel.showNote("Saved locally", false)
        viewModel.submit(
            RegistrationRequest(
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
            ),
        )
        advanceUntilIdle()
        viewModel.consumeError()
        assertTrue(viewModel.state is UiState.Idle)
        viewModel.clearOtp()
        assertEquals("", viewModel.otpNote)
    }

    @Test
    fun adminUsersSearchTheSubmittedQueryAndStopAtTheEnd() = runTest {
        val queries = mutableListOf<Pair<String, Int>>()
        val repository = object : AdminRepository by unusedAdmin() {
            override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> {
                queries += query to offset
                val item = summary(if (offset == 0) "user-1" else "user-2")
                return Result.success(AdminUserPage(listOf(item), total = 2, limit = 20, offset = offset))
            }
        }
        val viewModel = AdminUsersViewModel(repository)
        advanceUntilIdle()
        viewModel.onQueryChange("live text")
        assertEquals("", queries.last().first)
        viewModel.search()
        advanceUntilIdle()
        assertEquals("live text", queries.last().first)

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(1, queries.last().second)
        assertEquals(2, viewModel.users.size)
        val before = queries.size
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(before, queries.size)
    }

    @Test
    fun adminUserClearsThePersonWhenTheIdChangesAndUpdatesTheRole() = runTest {
        var fail = false
        val repository = object : AdminRepository by unusedAdmin() {
            override suspend fun getUser(id: String): Result<AdminUserDetail> =
                if (fail) Result.failure(Exception("missing")) else Result.success(detail(id, "participant"))

            override suspend fun setRole(id: String, role: String): Result<AdminUserDetail> =
                Result.success(detail(id, role))
        }
        val viewModel = AdminUserViewModel(repository)
        viewModel.load("user-1")
        advanceUntilIdle()
        viewModel.setRole("user-1", "admin")
        advanceUntilIdle()
        assertEquals("admin", viewModel.person?.profile?.role)

        fail = true
        viewModel.load("user-2")
        advanceUntilIdle()
        assertNull(viewModel.person)
        assertEquals("missing", viewModel.error)
    }

    @Test
    fun adminPlacesCapTheNameAndToggleActive() = runTest {
        var created = 0
        val repository = object : AdminRepository by unusedAdmin() {
            override suspend fun listPlaces(): Result<List<PlaceDto>> = Result.success(emptyList())
            override suspend fun createPlace(name: String): Result<PlaceDto> {
                created += 1
                return Result.success(place("place-1", name, true))
            }

            override suspend fun getPlace(id: String): Result<PlaceDto> = Result.success(place(id, "Gate", true))
            override suspend fun listPlaceAttendance(id: String, offset: Int): Result<AdminAttendanceListDto> =
                Result.success(AdminAttendanceListDto(emptyList(), 0, 50, offset))

            override suspend fun setPlaceActive(id: String, active: Boolean): Result<PlaceDto> =
                Result.success(place(id, "Gate", active))
        }
        val places = AdminPlacesViewModel(repository)
        advanceUntilIdle()
        places.onNameChange("A")
        places.create()
        advanceUntilIdle()
        assertEquals(0, created)

        places.onNameChange("A".repeat(200))
        assertEquals(120, places.name.length)
        places.create()
        advanceUntilIdle()
        assertEquals(1, created)
        assertEquals("", places.name)
        assertEquals(1, places.places.size)

        val place = AdminPlaceViewModel(repository)
        place.load("place-1")
        advanceUntilIdle()
        place.toggleActive()
        advanceUntilIdle()
        assertEquals(false, place.place?.active)
    }

    private fun profile(name: String) = ParticipantProfile(loginId = "SB-1", fullName = name, mobile = "9876543210")

    private fun visit(id: String) = AttendanceRecordDto(
        id = id,
        placeId = "place-1",
        placeName = "Gate",
        checkedInAt = "2026-11-03T10:13:00Z",
        attendanceDate = "2026-11-03",
        source = "qr",
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

    private fun summary(id: String) = AdminUserSummary(id, "SB-1", "Anita", "9876543210", null, null, "participant", "active")

    private fun detail(id: String, role: String) = AdminUserDetail(id, null, profile("Anita").copy(role = role))

    private fun place(id: String, name: String, active: Boolean) =
        PlaceDto(id, name, "004218", active, "2026-11-03T00:00:00Z")
}

private class MemorySession(start: ParticipantProfile?) : SessionStore {
    private var profile = start
    override fun isLoggedIn(): Boolean = true
    override fun accessToken(): String = "token"
    override fun cachedProfile(): ParticipantProfile? = profile
    override fun saveSession(token: String, profile: ParticipantProfile) {
        this.profile = profile
    }
    override fun saveProfile(profile: ParticipantProfile) {
        this.profile = profile
    }
    override fun clearSession() {
        profile = null
    }
}

private fun unusedAuth(): AuthRepository = object : AuthRepository {
    override suspend fun sendOtp(mobile: String): Result<String> = error("unused")
    override suspend fun confirmOtp(challengeId: String, code: String): Result<String> = error("unused")
    override suspend fun register(request: RegistrationRequest): Result<com.example.sahakarbharati.data.model.RegistrationResult> =
        error("unused")
    override suspend fun signIn(
        identifier: String,
        pin: String,
    ): Result<com.example.sahakarbharati.data.model.SessionResult> = error("unused")
    override suspend fun logout() = Unit
}

private fun unusedAdmin(): AdminRepository = object : AdminRepository {
    override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> = error("unused")
    override suspend fun getUser(id: String): Result<AdminUserDetail> = error("unused")
    override suspend fun setRole(id: String, role: String): Result<AdminUserDetail> = error("unused")
    override suspend fun listPlaces(): Result<List<PlaceDto>> = error("unused")
    override suspend fun getPlace(id: String): Result<PlaceDto> = error("unused")
    override suspend fun createPlace(name: String): Result<PlaceDto> = error("unused")
    override suspend fun setPlaceActive(id: String, active: Boolean): Result<PlaceDto> = error("unused")
    override suspend fun listPlaceAttendance(id: String, offset: Int) = error("unused")
}
