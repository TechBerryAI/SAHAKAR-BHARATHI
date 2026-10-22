package com.example.sahakarbharati.ui.navigation

object RathYatraDestinations {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val REGISTRATION_INTRO = "registration_intro"
    const val PHONE_OTP = "phone_otp"
    const val REGISTRATION_FORM = "registration_form"
    const val REGISTRATION_SUCCESS = "registration_success"
    const val HOME = "home"
    const val TRACKING = "tracking"
    const val QR_ATTENDANCE = "qr_attendance"
    const val QR_PASS = "qr_pass"
    const val SIGN_IN = "sign_in"
    const val BENEFITS = "benefits"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"

    val all: List<String> = listOf(
        SPLASH,
        WELCOME,
        REGISTRATION_INTRO,
        PHONE_OTP,
        REGISTRATION_FORM,
        REGISTRATION_SUCCESS,
        HOME,
        TRACKING,
        QR_ATTENDANCE,
        QR_PASS,
        SIGN_IN,
        BENEFITS,
        PROFILE,
        NOTIFICATIONS,
    )

    val startDestination: String = SPLASH

    val publicAccess: Set<String> = setOf(
        SPLASH,
        WELCOME,
        REGISTRATION_INTRO,
        PHONE_OTP,
        REGISTRATION_FORM,
        REGISTRATION_SUCCESS,
        SIGN_IN,
    )
}

fun navigateStack(
    stack: List<String>,
    destination: String,
    replaceRoot: Boolean = false,
): List<String> {
    val dest = if (destination in RathYatraDestinations.all) {
        destination
    } else {
        RathYatraDestinations.HOME
    }
    return when {
        replaceRoot -> listOf(dest)
        dest == RathYatraDestinations.HOME -> listOf(RathYatraDestinations.HOME)
        dest in stack -> {
            val trimmed = stack.dropLastWhile { it != dest }
            if (trimmed.isEmpty()) listOf(dest) else trimmed
        }
        else -> stack + dest
    }
}

fun popStack(stack: List<String>): List<String> {
    return if (stack.size > 1) stack.dropLast(1) else stack
}
