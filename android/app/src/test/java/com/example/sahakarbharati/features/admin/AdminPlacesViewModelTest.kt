package com.example.sahakarbharati.features.admin

import com.example.sahakarbharati.data.model.AdminUserDetail
import com.example.sahakarbharati.data.model.AdminUserPage
import com.example.sahakarbharati.data.remote.AdminAttendanceListDto
import com.example.sahakarbharati.data.remote.PlaceDto
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
import org.junit.Assert.assertNull
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
}

private class FakePlacesRepository(
    private val createResult: Result<PlaceDto>? = null,
) : AdminRepository {
    var createdName: String? = null

    override suspend fun listUsers(query: String, offset: Int): Result<AdminUserPage> =
        Result.failure(Exception("unused"))

    override suspend fun getUser(id: String): Result<AdminUserDetail> =
        Result.failure(Exception("unused"))

    override suspend fun setRole(id: String, role: String): Result<AdminUserDetail> =
        Result.failure(Exception("unused"))

    override suspend fun listPlaces(): Result<List<PlaceDto>> = Result.success(emptyList())

    override suspend fun getPlace(id: String): Result<PlaceDto> =
        Result.failure(Exception("unused"))

    override suspend fun createPlace(name: String): Result<PlaceDto> {
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
