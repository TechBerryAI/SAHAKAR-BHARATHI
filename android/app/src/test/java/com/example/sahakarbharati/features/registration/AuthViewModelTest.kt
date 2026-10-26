package com.example.sahakarbharati.features.registration

import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.RegistrationResult
import com.example.sahakarbharati.data.model.SessionResult
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
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
    fun registration_success_setsSuccessState() = runTest {
        val profile = sampleProfile()
        val repository = FakeAuthRepository(
            registerResult = Result.success(RegistrationResult("SB-ABC", profile)),
        )
        val viewModel = RegistrationViewModel(repository)
        viewModel.submit(sampleRequest())
        advanceUntilIdle()
        val state = viewModel.state
        assertTrue(state is UiState.Success)
        assertEquals("SB-ABC", (state as UiState.Success).data.registrationId)
    }

    @Test
    fun registration_error_setsErrorState() = runTest {
        val repository = FakeAuthRepository(
            registerResult = Result.failure(Exception("An account with these details already exists.")),
        )
        val viewModel = RegistrationViewModel(repository)
        viewModel.submit(sampleRequest())
        advanceUntilIdle()
        val state = viewModel.state
        assertTrue(state is UiState.Error)
        assertEquals("An account with these details already exists.", (state as UiState.Error).message)
    }

    @Test
    fun registration_startsInLoading() = runTest {
        val repository = FakeAuthRepository(
            registerResult = Result.success(RegistrationResult("SB-ABC", sampleProfile())),
        )
        val viewModel = RegistrationViewModel(repository)
        viewModel.submit(sampleRequest())
        assertTrue(viewModel.state is UiState.Loading)
        advanceUntilIdle()
    }

    @Test
    fun signIn_success_setsSuccessState() = runTest {
        val repository = FakeAuthRepository(
            signInResult = Result.success(SessionResult("token-1", sampleProfile())),
        )
        val viewModel = SignInViewModel(repository)
        viewModel.submit("9876543210", "2468")
        advanceUntilIdle()
        val state = viewModel.state
        assertTrue(state is UiState.Success)
        assertEquals("token-1", (state as UiState.Success).data.accessToken)
    }

    @Test
    fun signIn_error_setsErrorState() = runTest {
        val repository = FakeAuthRepository(
            signInResult = Result.failure(Exception("Mobile number or PIN is incorrect.")),
        )
        val viewModel = SignInViewModel(repository)
        viewModel.submit("9876543210", "0000")
        advanceUntilIdle()
        val state = viewModel.state
        assertTrue(state is UiState.Error)
        assertEquals("Login ID or PIN is incorrect.", (state as UiState.Error).message)
    }

    @Test
    fun signIn_startsInLoading() = runTest {
        val repository = FakeAuthRepository(
            signInResult = Result.success(SessionResult("token-1", sampleProfile())),
        )
        val viewModel = SignInViewModel(repository)
        viewModel.submit("9876543210", "2468")
        assertTrue(viewModel.state is UiState.Loading)
        advanceUntilIdle()
    }
}

private fun sampleProfile() = ParticipantProfile(
    loginId = "SB-ABC",
    fullName = "Anita Desai",
    mobile = "9876543210",
    occupation = "Farmer",
)

private fun sampleRequest() = RegistrationRequest(
    fullName = "Anita Desai",
    gender = "Female",
    dateOfBirth = "1985-03-12",
    occupation = "Farmer",
    mobile = "9876543210",
    email = "anita@example.com",
    addressLine = "12 Village Road",
    locality = "Ward 4",
    district = "Nashik",
    state = "Maharashtra",
    pincode = "422001",
    pin = "2468",
    verificationToken = "verification-token-value",
)

private class FakeAuthRepository(
    private val registerResult: Result<RegistrationResult> = Result.failure(Exception("unused")),
    private val signInResult: Result<SessionResult> = Result.failure(Exception("unused")),
) : AuthRepository {
    override suspend fun sendOtp(mobile: String) = Result.success("challenge")
    override suspend fun confirmOtp(challengeId: String, code: String) = Result.success("verification-token-value")
    override suspend fun register(request: RegistrationRequest) = registerResult
    override suspend fun signIn(identifier: String, pin: String) = signInResult
    override suspend fun logout() = Unit
}
