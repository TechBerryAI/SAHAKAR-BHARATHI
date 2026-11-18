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
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.repository.AdminRepository
import com.example.sahakarbharati.data.repository.AttendanceRepository
import com.example.sahakarbharati.data.repository.AuthRepository
import com.example.sahakarbharati.data.repository.ProfileRepository
import com.example.sahakarbharati.data.repository.SessionStore
import java.util.concurrent.CopyOnWriteArrayList
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

            override suspend fun mine(offset: Int): Result<AttendanceListDto> =
                Result.success(AttendanceListDto(listOf(visit("visit-1")), 1))

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
        assertEquals("otpSendFirst", viewModel.otpNoteKey)
        assertEquals("", viewModel.otpNote)

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
        viewModel.refresh()
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
            override suspend fun listPlaces(offset: Int): Result<com.example.sahakarbharati.data.remote.PlaceListDto> =
                Result.success(com.example.sahakarbharati.data.remote.PlaceListDto(emptyList()))
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

    @Test
    fun profileSaveIgnoresASecondTapUntilTheFirstRequestFinishes() = runTest {
        var updates = 0
        val repository = object : ProfileRepository {
            override suspend fun me(): Result<ParticipantProfile> = Result.success(profile("Anita"))
            override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> {
                updates += 1
                return Result.success(profile(request.fullName ?: "Anita"))
            }
        }
        val viewModel = ProfileViewModel(repository, unusedAuth())
        val request = ProfileUpdateRequest(fullName = "Anita Rao")
        viewModel.save(request)
        viewModel.save(request)
        advanceUntilIdle()
        assertEquals(1, updates)
        assertFalse(viewModel.saving)
        assertTrue(viewModel.saved)
    }

    @Test
    fun profileSaveRestoresTheButtonAfterAFailure() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun me(): Result<ParticipantProfile> = error("unused")
            override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> =
                Result.failure(Exception("Profile could not be updated."))
        }
        val viewModel = ProfileViewModel(repository, unusedAuth())
        viewModel.save(ProfileUpdateRequest(fullName = "Anita"))
        advanceUntilIdle()
        assertFalse(viewModel.saving)
        assertEquals("Profile could not be updated.", viewModel.saveError)
    }

    @Test
    fun attendanceHistoryPagesAppendAndKeepLoadedRows() = runTest {
        var calls = 0
        var failNext = false
        val repository = object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> = error("unused")
            override suspend fun mine(offset: Int): Result<AttendanceListDto> {
                calls += 1
                if (failNext && offset > 0) return Result.failure(Exception("page failed"))
                val items = List(if (offset == 0) 50 else 1) { index -> visit("visit-${offset + index}") }
                return Result.success(
                    AttendanceListDto(items, total = 51, limit = 50, offset = offset, hasMore = offset == 0),
                )
            }
        }
        val viewModel = AttendanceViewModel(repository)
        viewModel.load()
        advanceUntilIdle()
        assertEquals(50, viewModel.state.history.size)
        assertTrue(viewModel.state.hasMore)
        val first = viewModel.state.history.first().id

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(51, viewModel.state.history.size)
        assertEquals(first, viewModel.state.history.first().id)
        assertFalse(viewModel.state.hasMore)
        assertFalse(viewModel.state.loadingMore)

        val before = viewModel.state.history
        failNext = true
        calls = 0
        val stuck = AttendanceViewModel(object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String) = error("unused")
            override suspend fun mine(offset: Int): Result<AttendanceListDto> {
                calls += 1
                return if (offset == 0) {
                    Result.success(AttendanceListDto(listOf(visit("visit-1")), total = 2, limit = 50, offset = 0, hasMore = true))
                } else {
                    Result.failure(Exception("page failed"))
                }
            }
        })
        stuck.load()
        advanceUntilIdle()
        stuck.loadMore()
        stuck.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(listOf("visit-1"), stuck.state.history.map { it.id })
        assertEquals("page failed", stuck.state.error)
        assertEquals(before.first().id, viewModel.state.history.first().id)
    }

    @Test
    fun attendanceHistoryCoversEmptyAndAFullLastPage() = runTest {
        val empty = AttendanceViewModel(object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> = error("unused")
            override suspend fun mine(offset: Int): Result<AttendanceListDto> =
                Result.success(AttendanceListDto(emptyList(), total = 0, limit = 50, offset = 0, hasMore = false))
        })
        empty.load()
        advanceUntilIdle()
        assertTrue(empty.state.history.isEmpty())
        assertFalse(empty.state.hasMore)
        empty.loadMore()
        advanceUntilIdle()
        assertTrue(empty.state.history.isEmpty())

        var offsets = mutableListOf<Int>()
        val exact = AttendanceViewModel(object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> = error("unused")
            override suspend fun mine(offset: Int): Result<AttendanceListDto> {
                offsets += offset
                val items = List(50) { visit("visit-$it") }
                return Result.success(AttendanceListDto(items, total = 50, limit = 50, offset = offset, hasMore = false))
            }
        })
        exact.load()
        advanceUntilIdle()
        exact.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(0), offsets)
        assertEquals(50, exact.state.history.size)
        assertFalse(exact.state.hasMore)
    }

    @Test
    fun openingPeopleRefreshesPageOneWithoutRepeatingOnTheSameEntry() = runTest {
        var calls = 0
        val repository = object : AdminRepository by unusedAdmin() {
            override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> {
                calls += 1
                val item = summary(if (offset == 0) "user-1" else "user-2")
                return Result.success(AdminUserPage(listOf(item), total = 2, limit = 20, offset = offset))
            }
        }
        val viewModel = AdminUsersViewModel(repository)
        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, calls)
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(2, viewModel.users.size)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("user-1"), viewModel.users.map { it.id })
        assertEquals(3, calls)
    }

    @Test
    fun logoutClearsProfileAttendanceAndAdminStateBeforeTheNextAccount() = runTest {
        val profiles = object : ProfileRepository {
            override suspend fun me(): Result<ParticipantProfile> = Result.success(profile("Anita"))
            override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> =
                Result.success(profile(request.fullName ?: "Anita"))
        }
        val attendance = object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> = error("unused")
            override suspend fun mine(offset: Int): Result<AttendanceListDto> =
                Result.success(AttendanceListDto(listOf(visit("visit-a")), total = 1))
        }
        val admin = object : AdminRepository by unusedAdmin() {
            override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> =
                Result.success(AdminUserPage(listOf(summary("user-a")), total = 1, limit = 20, offset = 0))
            override suspend fun listPlaces(offset: Int): Result<com.example.sahakarbharati.data.remote.PlaceListDto> =
                Result.success(com.example.sahakarbharati.data.remote.PlaceListDto(listOf(place("place-a", "Gate", true)), total = 1))
        }
        val session = NotifyingSession()
        session.saveSession("token-a", profile("Anita"))
        val profileViewModel = ProfileViewModel(profiles, unusedAuth(), session)
        val homeViewModel = HomeViewModel(profiles, session)
        val attendanceViewModel = AttendanceViewModel(attendance, session)
        val peopleViewModel = AdminUsersViewModel(admin, session)
        val boothsViewModel = AdminPlacesViewModel(admin, session)
        profileViewModel.load()
        homeViewModel.load()
        attendanceViewModel.load()
        peopleViewModel.refresh()
        boothsViewModel.refresh()
        advanceUntilIdle()
        assertEquals("Anita", (profileViewModel.state as UiState.Success).data.fullName)
        assertEquals("Anita", (homeViewModel.state as UiState.Success).data.fullName)
        assertEquals(1, attendanceViewModel.state.history.size)
        assertEquals(1, peopleViewModel.users.size)
        assertEquals(1, boothsViewModel.places.size)

        session.clearSession()
        assertTrue(profileViewModel.state is UiState.Idle)
        assertTrue(homeViewModel.state is UiState.Idle)
        assertTrue(attendanceViewModel.state.history.isEmpty())
        assertTrue(peopleViewModel.users.isEmpty())
        assertTrue(boothsViewModel.places.isEmpty())
        assertNull(session.cachedProfile())

        session.saveSession("token-b", profile("Bina"))
        val bina = object : ProfileRepository {
            override suspend fun me(): Result<ParticipantProfile> = Result.success(profile("Bina"))
            override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> = error("unused")
        }
        val nextProfile = ProfileViewModel(bina, unusedAuth(), session)
        nextProfile.load()
        advanceUntilIdle()
        assertEquals("Bina", (nextProfile.state as UiState.Success).data.fullName)
        assertTrue(profileViewModel.state is UiState.Idle)
    }

    @Test
    fun aDelayedPreviousAccountResponseCannotFillTheNextAccount() = runTest {
        val gate = CompletableDeferred<Result<ParticipantProfile>>()
        var calls = 0
        val profiles = object : ProfileRepository {
            override suspend fun me(): Result<ParticipantProfile> {
                calls += 1
                return if (calls == 1) gate.await() else Result.success(profile("Bina"))
            }
            override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> = error("unused")
        }
        val session = NotifyingSession()
        session.saveSession("token-a", profile("Anita"))
        val viewModel = ProfileViewModel(profiles, unusedAuth(), session)
        viewModel.load()
        runCurrent()
        session.saveSession("token-b", profile("Bina"))
        gate.complete(Result.success(profile("Anita")))
        viewModel.load()
        advanceUntilIdle()
        assertEquals("Bina", (viewModel.state as UiState.Success).data.fullName)

        val historyGate = CompletableDeferred<Result<AttendanceListDto>>()
        var historyCalls = 0
        val attendance = object : AttendanceRepository {
            override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> = error("unused")
            override suspend fun mine(offset: Int): Result<AttendanceListDto> {
                historyCalls += 1
                return if (historyCalls == 1) {
                    historyGate.await()
                } else {
                    Result.success(AttendanceListDto(listOf(visit("visit-b")), total = 1))
                }
            }
        }
        val attendanceViewModel = AttendanceViewModel(attendance, session)
        attendanceViewModel.load()
        runCurrent()
        session.clearSession()
        historyGate.complete(Result.success(AttendanceListDto(listOf(visit("visit-a")), total = 1)))
        advanceUntilIdle()
        assertTrue(attendanceViewModel.state.history.isEmpty())
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

private class NotifyingSession : SessionStore {
    private val listeners = CopyOnWriteArrayList<(Int) -> Unit>()
    private var epoch = 0
    private var token: String? = null
    private var profile: ParticipantProfile? = null

    override fun isLoggedIn(): Boolean = !token.isNullOrBlank()
    override fun accessToken(): String? = token
    override fun cachedProfile(): ParticipantProfile? = profile
    override fun epoch(): Int = epoch
    override fun addEpochListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }
    override fun removeEpochListener(listener: (Int) -> Unit) {
        listeners.remove(listener)
    }
    override fun saveSession(token: String, profile: ParticipantProfile) {
        this.token = token
        this.profile = profile
        advance()
    }
    override fun saveProfile(profile: ParticipantProfile) {
        this.profile = profile
    }
    override fun clearSession() {
        token = null
        profile = null
        advance()
    }
    private fun advance() {
        epoch += 1
        val current = epoch
        listeners.forEach { listener -> listener(current) }
    }
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
    override suspend fun listPlaces(offset: Int): Result<com.example.sahakarbharati.data.remote.PlaceListDto> = error("unused")
    override suspend fun getPlace(id: String): Result<PlaceDto> = error("unused")
    override suspend fun createPlace(name: String): Result<PlaceDto> = error("unused")
    override suspend fun setPlaceActive(id: String, active: Boolean): Result<PlaceDto> = error("unused")
    override suspend fun listPlaceAttendance(id: String, offset: Int) = error("unused")
}
