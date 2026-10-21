package com.example.sahakarbharati.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

class AppSession(private val prefs: SharedPreferences) {
    val isRegistered: Boolean
        get() = prefs.getBoolean(KEY_REGISTERED, false)

    val isLoggedIn: Boolean
        get() = isRegistered && prefs.getBoolean(KEY_LOGGED_IN, false)

    val loginId: String
        get() = prefs.getString(KEY_LOGIN_ID, "") ?: ""

    val mobile: String
        get() = prefs.getString(KEY_MOBILE, "") ?: ""

    val name: String
        get() = prefs.getString(KEY_NAME, "") ?: ""

    val photoPath: String
        get() = prefs.getString(KEY_PHOTO, "") ?: ""

    val displayPhotoPath: String
        get() = photoPath.ifBlank { DemoPhotos.BOY }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
        }

    fun storePhoto(context: Context, uri: Uri): String? {
        val dest = File(context.filesDir, PHOTO_FILE)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            prefs.edit().putString(KEY_PHOTO, dest.absolutePath).apply()
            dest.absolutePath
        }.getOrNull()
    }

    fun setPhotoRef(ref: String) {
        prefs.edit().putString(KEY_PHOTO, ref).apply()
    }

    fun ensureDefaultPhoto() {
        if (photoPath.isBlank()) setPhotoRef(DemoPhotos.BOY)
    }

    fun completeRegistration(
        name: String,
        mobile: String,
        pin: String,
        loginId: String = DEFAULT_LOGIN_ID,
        photoPath: String = this.photoPath,
    ) {
        prefs.edit()
            .putBoolean(KEY_REGISTERED, true)
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_NAME, name)
            .putString(KEY_MOBILE, mobile.filter(Char::isDigit))
            .putString(KEY_PIN, pin)
            .putString(KEY_LOGIN_ID, loginId)
            .putString(KEY_PHOTO, photoPath)
            .apply()
    }

    fun login(id: String, pin: String): Boolean {
        if (!isRegistered) return false
        val storedPin = prefs.getString(KEY_PIN, "") ?: return false
        if (pin != storedPin) return false
        val typedDigits = id.filter(Char::isDigit)
        val typedKey = id.filter { it.isLetterOrDigit() }.lowercase()
        val mobileKey = mobile.filter(Char::isDigit)
        val loginKey = loginId.filter { it.isLetterOrDigit() }.lowercase()
        val matches = typedKey == loginKey || (typedDigits.isNotEmpty() && typedDigits == mobileKey)
        if (matches) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        }
        return matches
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    companion object {
        const val DEFAULT_LOGIN_ID = "FF-IND-1042"
        private const val PREFS = "sahakar_session"
        private const val KEY_REGISTERED = "registered"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NAME = "name"
        private const val KEY_MOBILE = "mobile"
        private const val KEY_PIN = "pin"
        private const val KEY_LOGIN_ID = "login_id"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_PHOTO = "photo_path"
        private const val PHOTO_FILE = "profile_photo.jpg"

        fun from(context: Context): AppSession =
            AppSession(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
    }
}

object DemoPhotos {
    const val PREFIX = "drawable:"
    const val BOY = "drawable:img_demo_boy"
    const val GIRL = "drawable:img_demo_girl"

    fun isDemo(path: String?) = path == BOY || path == GIRL

    fun drawableName(ref: String): String? =
        ref.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)

    fun forGender(gender: String) = when (gender) {
        "Female" -> GIRL
        else -> BOY
    }
}

@Composable
fun rememberAppSession(): AppSession {
    val context = LocalContext.current
    return remember {
        AppSession.from(context).also { it.ensureDefaultPhoto() }
    }
}
