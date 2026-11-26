package com.example.sahakarbharati.ui.navigation

object RathYatraDestinations {
    const val SPLASH = "splash"
    const val REGISTRATION_FORM = "registration_form"
    const val REGISTRATION_SUCCESS = "registration_success"
    const val HOME = "home"
    const val SIGN_IN = "sign_in"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val ADMIN_USER = "admin_user"
    const val ADMIN_PLACES = "admin_places"
    const val ADMIN_PLACE = "admin_place"
    const val ATTENDANCE = "attendance"
    const val YATRAS = "yatras"
    const val YATRA_DETAIL = "yatra_detail"
    const val YATRA_ATTENDANCE = "yatra_attendance"
    const val YATRA_TRACKING = "yatra_tracking"

    val all: List<String> = listOf(
        SPLASH,
        REGISTRATION_FORM,
        REGISTRATION_SUCCESS,
        HOME,
        SIGN_IN,
        PROFILE,
        ADMIN,
        ADMIN_USER,
        ADMIN_PLACES,
        ADMIN_PLACE,
        ATTENDANCE,
        YATRAS,
        YATRA_DETAIL,
        YATRA_ATTENDANCE,
        YATRA_TRACKING,
    )

    val startDestination: String = SPLASH

    val publicAccess: Set<String> = setOf(
        SPLASH,
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
