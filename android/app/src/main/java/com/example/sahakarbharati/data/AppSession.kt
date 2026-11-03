package com.example.sahakarbharati.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.sahakarbharati.SahakarApp
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.knownRole
import com.example.sahakarbharati.data.repository.SessionStore
import java.io.File

class AppSession(
    private val securePrefs: SharedPreferences,
    private val localPrefs: SharedPreferences,
) : SessionStore {
    private val signedInState = mutableStateOf(!accessToken().isNullOrBlank())

    /** Observable from Compose; turns false when the server rejects the stored token. */
    val signedIn: State<Boolean>
        get() = signedInState

    val isRegistered: Boolean
        get() = !loginId.isBlank()

    val loginId: String
        get() = securePrefs.getString(KEY_LOGIN_ID, "") ?: ""

    val mobile: String
        get() = securePrefs.getString(KEY_MOBILE, "") ?: ""

    val name: String
        get() = securePrefs.getString(KEY_NAME, "") ?: ""

    val occupation: String
        get() = securePrefs.getString(KEY_OCCUPATION, "") ?: ""

    val email: String
        get() = securePrefs.getString(KEY_EMAIL, "") ?: ""

    val district: String
        get() = securePrefs.getString(KEY_DISTRICT, "") ?: ""

    val state: String
        get() = securePrefs.getString(KEY_STATE, "") ?: ""

    fun role(): String = knownRole(securePrefs.getString(KEY_ROLE, null))

    /** Device preference. Sign-out does not clear it, matching the website. */
    fun languageKey(): String = localPrefs.getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() } ?: "en"

    fun setLanguageKey(key: String) {
        localPrefs.edit().putString(KEY_LANGUAGE, key).apply()
    }

    val photoPath: String
        get() = localPrefs.getString(KEY_PHOTO, "") ?: ""

    val displayPhotoPath: String
        get() = photoPath

    var notificationsEnabled: Boolean
        get() = localPrefs.getBoolean(KEY_NOTIFICATIONS, false)
        set(value) {
            localPrefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
        }

    override fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank()

    override fun accessToken(): String? = securePrefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    override fun cachedProfile(): ParticipantProfile? {
        if (loginId.isBlank() && name.isBlank()) return null
        return ParticipantProfile(
            loginId = loginId,
            fullName = name,
            mobile = mobile,
            email = email.ifBlank { null },
            gender = blankToNull(KEY_GENDER),
            dateOfBirth = blankToNull(KEY_DOB),
            occupation = occupation.ifBlank { null },
            addressLine = blankToNull(KEY_ADDRESS),
            locality = blankToNull(KEY_LOCALITY),
            district = district.ifBlank { null },
            state = state.ifBlank { null },
            pincode = blankToNull(KEY_PINCODE),
            status = blankToNull(KEY_STATUS),
            role = role(),
        )
    }

    override fun saveSession(token: String, profile: ParticipantProfile) {
        writeProfile(securePrefs.edit().putString(KEY_TOKEN, token), profile).apply()
        signedInState.value = true
    }

    override fun saveProfile(profile: ParticipantProfile) {
        writeProfile(securePrefs.edit(), profile).apply()
    }

    private fun blankToNull(key: String): String? = securePrefs.getString(key, "")?.ifBlank { null }

    private fun writeProfile(
        editor: SharedPreferences.Editor,
        profile: ParticipantProfile,
    ): SharedPreferences.Editor = editor
        .putString(KEY_LOGIN_ID, profile.loginId)
        .putString(KEY_NAME, profile.fullName)
        .putString(KEY_MOBILE, profile.mobile)
        .putString(KEY_EMAIL, profile.email.orEmpty())
        .putString(KEY_GENDER, profile.gender.orEmpty())
        .putString(KEY_DOB, profile.dateOfBirth.orEmpty())
        .putString(KEY_OCCUPATION, profile.occupation.orEmpty())
        .putString(KEY_ADDRESS, profile.addressLine.orEmpty())
        .putString(KEY_LOCALITY, profile.locality.orEmpty())
        .putString(KEY_DISTRICT, profile.district.orEmpty())
        .putString(KEY_STATE, profile.state.orEmpty())
        .putString(KEY_PINCODE, profile.pincode.orEmpty())
        .putString(KEY_STATUS, profile.status.orEmpty())
        .putString(KEY_ROLE, knownRole(profile.role))

    override fun clearSession() {
        securePrefs.edit().clear().apply()
        localPrefs.edit()
            .remove(KEY_PHOTO)
            .putBoolean(KEY_NOTIFICATIONS, false)
            .apply()
        signedInState.value = false
    }

    /** Clears the session only if [token] is still the stored one, so a late 401 cannot end a newer session. */
    fun clearIfCurrent(token: String) {
        if (accessToken() == token) {
            clearSession()
        }
    }

    fun storePhoto(context: Context, uri: Uri): String? {
        val dest = File(context.filesDir, PHOTO_FILE)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            localPrefs.edit().putString(KEY_PHOTO, dest.absolutePath).apply()
            dest.absolutePath
        }.getOrNull()
    }

    companion object {
        const val SECURE_PREFS = "sahakar_secure_session"
        const val LOCAL_PREFS = "sahakar_session"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_LOGIN_ID = "login_id"
        private const val KEY_NAME = "name"
        private const val KEY_MOBILE = "mobile"
        private const val KEY_EMAIL = "email"
        private const val KEY_OCCUPATION = "occupation"
        private const val KEY_DISTRICT = "district"
        private const val KEY_STATE = "state"
        private const val KEY_GENDER = "gender"
        private const val KEY_DOB = "date_of_birth"
        private const val KEY_ADDRESS = "address_line"
        private const val KEY_LOCALITY = "locality"
        private const val KEY_PINCODE = "pincode"
        private const val KEY_STATUS = "status"
        private const val KEY_ROLE = "role"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_PHOTO = "photo_path"
        private const val PHOTO_FILE = "profile_photo.jpg"

        fun from(context: Context): AppSession {
            val appContext = context.applicationContext
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val secure = EncryptedSharedPreferences.create(
                SECURE_PREFS,
                masterKey,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            val local = appContext.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
            local.edit().remove("pin").apply()
            return AppSession(secure, local)
        }
    }
}

object DemoPhotos {
    const val PREFIX = "drawable:"
    const val BOY = "drawable:img_demo_boy"
    const val GIRL = "drawable:img_demo_girl"

    fun isDemo(path: String?) = path == BOY || path == GIRL

    fun drawableName(ref: String): String? =
        ref.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)
}

/** Whether a request is taking long enough to show the "server is starting up" banner. */
@Composable
fun rememberServerSlow(): State<Boolean> {
    val context = LocalContext.current
    return remember {
        (context.applicationContext as? SahakarApp)?.container?.slowRequests?.serverSlow ?: mutableStateOf(false)
    }
}

@Composable
fun rememberAppSession(): AppSession {
    val context = LocalContext.current
    return remember {
        val app = context.applicationContext
        if (app is SahakarApp) app.container.session else AppSession.from(context)
    }
}
