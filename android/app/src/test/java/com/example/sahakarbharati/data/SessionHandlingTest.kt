package com.example.sahakarbharati.data

import android.content.SharedPreferences
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmRequest
import com.example.sahakarbharati.data.remote.PhoneVerificationConfirmedDto
import com.example.sahakarbharati.data.remote.PhoneVerificationCreatedDto
import com.example.sahakarbharati.data.remote.PhoneVerificationRequest
import com.example.sahakarbharati.data.remote.ProfileResponseDto
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.remote.RegistrationResponseDto
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.SessionExpiryInterceptor
import com.example.sahakarbharati.data.remote.SessionRequest
import com.example.sahakarbharati.data.remote.SessionResponseDto
import com.example.sahakarbharati.data.repository.ProfileRepository
import com.example.sahakarbharati.data.repository.RemoteAuthRepository
import com.example.sahakarbharati.features.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHandlingTest {
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
    fun unauthorizedResponseClearsTheMatchingSession() {
        val session = newSession()
        session.saveSession("token-1", profile)
        assertTrue(session.signedIn.value)

        call(session, token = "token-1", code = 401)

        assertFalse(session.signedIn.value)
        assertNull(session.accessToken())
        assertNull(session.cachedProfile())
    }

    @Test
    fun lateUnauthorizedResponseForAnOldTokenKeepsTheNewSession() {
        val session = newSession()
        session.saveSession("new-token", profile)

        call(session, token = "old-token", code = 401)

        assertTrue(session.signedIn.value)
        assertEquals("new-token", session.accessToken())
    }

    @Test
    fun serverErrorsDoNotEndTheSession() {
        val session = newSession()
        session.saveSession("token-1", profile)

        call(session, token = "token-1", code = 503)

        assertTrue(session.signedIn.value)
    }

    @Test
    fun homeKeepsCachedProfileWhenOffline() = runTest {
        val session = newSession()
        session.saveSession("token-1", profile)
        val viewModel = HomeViewModel(FailingProfileRepository(), session)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(profile.loginId, (viewModel.state as UiState.Success).data.loginId)
    }

    @Test
    fun homeDoesNotShowCachedProfileAfterSessionRejected() = runTest {
        val session = newSession()
        session.saveSession("token-1", profile)
        val repository = FailingProfileRepository(onFail = { session.clearIfCurrent("token-1") })
        val viewModel = HomeViewModel(repository, session)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state is UiState.Error)
    }

    @Test
    fun registrationSucceededButSignInFailedIsNotReportedAsRegistrationFailure() = runTest {
        val session = newSession()
        val repository = RemoteAuthRepository(FakeApi(sessionFails = true), session)

        val result = repository.register(registrationRequest)

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains(registrationRequest.mobile))
        assertTrue(message, message.contains("mobile number and PIN"))
        assertFalse(message, message.startsWith("Registration failed"))
        assertFalse(session.isLoggedIn())
    }

    @Test
    fun registrationSuccessStoresTheSession() = runTest {
        val session = newSession()
        val repository = RemoteAuthRepository(FakeApi(sessionFails = false), session)

        val result = repository.register(registrationRequest)

        assertEquals("SB-1234ABCD", result.getOrThrow().registrationId)
        assertEquals("token-1", session.accessToken())
    }

    private fun call(session: AppSession, token: String, code: Int) {
        val client = OkHttpClient.Builder()
            .addInterceptor(SessionExpiryInterceptor(session))
            .addInterceptor(StatusInterceptor(code))
            .build()
        val request = Request.Builder()
            .url("https://api.test.invalid/api/v1/me")
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().close()
    }
}

private val profile = ParticipantProfile(loginId = "SB-1234ABCD", fullName = "Anita Desai", mobile = "9876543210")

private val registrationRequest = RegistrationRequest(
    fullName = "Anita Desai",
    gender = null,
    dateOfBirth = null,
    occupation = "Farmer",
    mobile = "9876543210",
    email = null,
    addressLine = null,
    locality = null,
    district = null,
    state = null,
    pincode = null,
    pin = "2468",
    verificationToken = "verification-token-value",
)

private fun newSession() = AppSession(InMemoryPreferences(), InMemoryPreferences())

/** Answers every request locally with [code]; no network. */
private class StatusInterceptor(private val code: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("status $code")
            .body("{\"detail\":\"status $code\"}".toResponseBody("application/json".toMediaType()))
            .build()
}

private class FailingProfileRepository(private val onFail: () -> Unit = {}) : ProfileRepository {
    override suspend fun me(): Result<ParticipantProfile> {
        onFail()
        return Result.failure(Exception("The server could not be reached."))
    }

    override suspend fun update(request: ProfileUpdateRequest): Result<ParticipantProfile> =
        Result.failure(Exception("unused"))
}

private class FakeApi(private val sessionFails: Boolean) : SahakarApi {
    override suspend fun sendPhoneVerification(body: PhoneVerificationRequest) = PhoneVerificationCreatedDto("id", 300)

    override suspend fun confirmPhoneVerification(challengeId: String, body: PhoneVerificationConfirmRequest) =
        PhoneVerificationConfirmedDto("verification-token-value", 900)

    override suspend fun register(body: RegistrationRequest) = RegistrationResponseDto(
        registrationId = "SB-1234ABCD",
        loginId = "SB-1234ABCD",
        fullName = body.fullName,
        mobile = body.mobile,
        occupation = body.occupation,
        state = null,
        district = null,
    )

    override suspend fun createSession(body: SessionRequest): SessionResponseDto {
        if (sessionFails) {
            throw HttpException(retrofit2.Response.error<Any>(503, "{}".toResponseBody("application/json".toMediaType())))
        }
        return SessionResponseDto("token-1", "bearer", null, "SB-1234ABCD", "Anita Desai", body.identifier, "Farmer", null, null)
    }

    override suspend fun logout(authorization: String) = Unit

    override suspend fun me(authorization: String): ProfileResponseDto = error("unused")

    override suspend fun updateMe(authorization: String, body: ProfileUpdateRequest): ProfileResponseDto = error("unused")
}

private class InMemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
    override fun contains(key: String): Boolean = key in values
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun remove(key: String) = apply { removed += key }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) values.clear()
            removed.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}
