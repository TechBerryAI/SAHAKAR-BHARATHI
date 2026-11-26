package com.example.sahakarbharati.features.admin

import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserPage
import com.example.sahakarbharati.data.remote.AdminAttendanceListDto
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.remote.PlaceListDto
import com.example.sahakarbharati.data.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class AdminPlacesViewModelTest {
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
    fun shortName_isRejectedAndDoesNotCallTheApi() = runTest {
        val repository = FakePlacesRepository()
        val viewModel = AdminPlacesViewModel(repository)
        advanceUntilIdle()
        viewModel.onNameChange("A")
        viewModel.create()
        assertEquals("Booth name is required.", viewModel.error)
        assertNull(repository.createdName)
    }

    @Test
    fun validName_createsTheBoothAndClearsTheField() = runTest {
        val repository = FakePlacesRepository()
        val viewModel = AdminPlacesViewModel(repository)
        advanceUntilIdle()
        viewModel.onNameChange("  Pune stall  ")
        viewModel.create()
        advanceUntilIdle()
        assertEquals("Pune stall", repository.createdName)
        assertEquals("", viewModel.name)
        assertEquals("Pune stall", viewModel.places.first().name)
        assertEquals("274097", viewModel.places.first().code)
        assertNull(viewModel.error)
    }

    @Test
    fun apiFailure_isShown() = runTest {
        val repository = FakePlacesRepository(
            createResult = Result.failure(Exception("Could not create this booth.")),
        )
        val viewModel = AdminPlacesViewModel(repository)
        advanceUntilIdle()
        viewModel.onNameChange("Pune stall")
        viewModel.create()
        advanceUntilIdle()
        assertEquals("Could not create this booth.", viewModel.error)
        assertEquals("Pune stall", viewModel.name)
    }

    @Test
    fun twoRapidCreatesSendOneRequestAndAFailureClearsSaving() = runTest {
        val repository = FakePlacesRepository()
        val viewModel = AdminPlacesViewModel(repository)
        viewModel.onNameChange("Pune stall")
        viewModel.create()
        viewModel.create()
        advanceUntilIdle()
        assertEquals(1, repository.creates)
        assertFalse(viewModel.saving)

        val failing = FakePlacesRepository(Result.failure(Exception("Could not create this booth.")))
        val failed = AdminPlacesViewModel(failing)
        failed.onNameChange("Pune stall")
        failed.create()
        advanceUntilIdle()
        assertFalse(failed.saving)
        assertEquals("Could not create this booth.", failed.error)
    }

    @Test
    fun boothPagesAppendAndAFailedPageKeepsTheFirstPage() = runTest {
        var calls = 0
        var failNext = false
        val repository = object : AdminRepository by unusedPlaces() {
            override suspend fun listPlaces(offset: Int): Result<PlaceListDto> {
                calls += 1
                if (failNext) return Result.failure(Exception("page failed"))
                val item = PlaceDto("place-$offset", "Booth $offset", "10000$offset", true, "2026-11-13T06:00:00Z")
                return Result.success(
                    PlaceListDto(listOf(item), total = 3, limit = 100, offset = offset, hasMore = offset + 1 < 3),
                )
            }
        }
        val viewModel = AdminPlacesViewModel(repository)
        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, calls)
        assertEquals(listOf("place-0"), viewModel.places.map { it.id })
        assertTrue(viewModel.hasMore)

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(listOf("place-0", "place-1"), viewModel.places.map { it.id })
        assertTrue(viewModel.hasMore)

        failNext = true
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("place-0", "place-1"), viewModel.places.map { it.id })
        assertEquals("page failed", viewModel.error)
    }

    @Test
    fun openingBoothsReplacesTheListWithPageOne() = runTest {
        val repository = object : AdminRepository by unusedPlaces() {
            override suspend fun listPlaces(offset: Int): Result<PlaceListDto> {
                val item = PlaceDto(
                    if (offset == 0) "place-1" else "place-2",
                    "Booth",
                    "274097",
                    true,
                    "2026-11-13T06:00:00Z",
                )
                return Result.success(PlaceListDto(listOf(item), total = 2, limit = 100, offset = offset, hasMore = offset == 0))
            }
        }
        val viewModel = AdminPlacesViewModel(repository)
        viewModel.refresh()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(2, viewModel.places.size)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("place-1"), viewModel.places.map { it.id })
    }
}

private fun unusedPlaces(): AdminRepository = FakePlacesRepository()

private class FakePlacesRepository(
    private val createResult: Result<PlaceDto>? = null,
) : AdminRepository {
    var createdName: String? = null
    var creates = 0

    override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> =
        Result.failure(Exception("unused"))

    override suspend fun getUser(id: String): Result<AdminUserDetail> =
        Result.failure(Exception("unused"))

    override suspend fun setRole(id: String, role: String): Result<AdminUserDetail> =
        Result.failure(Exception("unused"))

    override suspend fun listPlaces(offset: Int): Result<PlaceListDto> =
        Result.success(PlaceListDto(emptyList(), total = 0))

    override suspend fun getPlace(id: String): Result<PlaceDto> =
        Result.failure(Exception("unused"))

    override suspend fun createPlace(name: String): Result<PlaceDto> {
        creates += 1
        createdName = name
        return createResult ?: Result.success(
            PlaceDto(
                id = "place-1",
                name = name,
                code = "274097",
                active = true,
                createdAt = "2026-11-13T06:00:00Z",
            ),
        )
    }

    override suspend fun setPlaceActive(id: String, active: Boolean): Result<PlaceDto> =
        Result.failure(Exception("unused"))

    override suspend fun listPlaceAttendance(id: String, offset: Int): Result<AdminAttendanceListDto> =
        Result.failure(Exception("unused"))
}
