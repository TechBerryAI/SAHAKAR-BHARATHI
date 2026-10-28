package com.example.sahakarbharati.features.registration

import com.example.sahakarbharati.data.model.RegistrationResult
import com.example.sahakarbharati.data.model.SessionResult
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.remote.mapApiError
import com.example.sahakarbharati.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OtpInputTest {
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
    fun malformedCodesAreRejectedLocallyWithoutCallingTheServer() = runTest {
        val repository = CountingAuthRepository()
        val viewModel = RegistrationViewModel(repository)
        viewModel.sendOtp("9876543210")
        advanceUntilIdle()

        listOf("", "12", "12345a", "1234567").forEach { code ->
            viewModel.confirmOtp(code)
            advanceUntilIdle()
            assertEquals(code, "Enter the 6-digit code from the SMS.", viewModel.otpNote)
        }
        assertEquals(0, repository.confirmCalls)
        assertNull(viewModel.verificationToken)

        viewModel.confirmOtp("123456")
        advanceUntilIdle()
        assertEquals(1, repository.confirmCalls)
        assertEquals("verification-token-value", viewModel.verificationToken)
    }

    @Test
    fun validatorPrefixIsStrippedFromServerMessages() {
        val body = """{"detail":[{"msg":"Value error, Enter a valid 10-digit Indian mobile number."}]}"""
        val error = HttpException(Response.error<Any>(422, body.toResponseBody("application/json".toMediaType())))
        assertEquals("Enter a valid 10-digit Indian mobile number.", mapApiError(error, "fallback"))
    }
}

private class CountingAuthRepository : AuthRepository {
    var confirmCalls = 0

    override suspend fun sendOtp(mobile: String) = Result.success("challenge")

    override suspend fun confirmOtp(challengeId: String, code: String): Result<String> {
        confirmCalls += 1
        return Result.success("verification-token-value")
    }

    override suspend fun register(request: RegistrationRequest): Result<RegistrationResult> = Result.failure(Exception("unused"))

    override suspend fun signIn(identifier: String, pin: String): Result<SessionResult> = Result.failure(Exception("unused"))

    override suspend fun logout() = Unit
}
