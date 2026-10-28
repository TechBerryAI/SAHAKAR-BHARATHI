package com.example.sahakarbharati.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.data.rememberServerSlow
import com.example.sahakarbharati.ui.theme.RathYatraColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sahakarbharati.core.ui.DemoUnavailableScreen
import com.example.sahakarbharati.features.tracking.ChariotTrackingScreen
import com.example.sahakarbharati.features.home.CitizenHomeScreen
import com.example.sahakarbharati.features.profile.ProfileScreen
import com.example.sahakarbharati.features.attendance.QrAttendanceScreen
import com.example.sahakarbharati.features.registration.RegistrationFormScreen
import com.example.sahakarbharati.features.registration.RegistrationSuccessScreen
import com.example.sahakarbharati.features.registration.SignInScreen
import com.example.sahakarbharati.features.onboarding.SplashStartScreen
import com.example.sahakarbharati.features.onboarding.WelcomeDiscoveryScreen
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.LocalSetLanguage
import com.example.sahakarbharati.core.copy.copyFor
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.features.profile.ProfileViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RathYatraScreen(
    modifier: Modifier = Modifier,
    initialStack: List<String> = listOf(RathYatraDestinations.startDestination),
) {
    val session = rememberAppSession()
    val serverSlow by rememberServerSlow()
    val profileViewModel: ProfileViewModel = sahakarViewModel()
    val scope = rememberCoroutineScope()
    val openedOnProtectedScreen = initialStack.any { it !in RathYatraDestinations.publicAccess }
    var stack by rememberSaveable { mutableStateOf(initialStack.ifEmpty { listOf(RathYatraDestinations.SPLASH) }) }
    var languageKey by rememberSaveable { mutableStateOf(AppLanguage.EN.key) }
    var loggedIn by rememberSaveable { mutableStateOf(session.isLoggedIn() || openedOnProtectedScreen) }
    val language = AppLanguage.fromKey(languageKey)
    val copy = copyFor(language)
    val current = stack.lastOrNull() ?: RathYatraDestinations.SPLASH

    fun canOpen(destination: String): Boolean {
        return destination in RathYatraDestinations.publicAccess || loggedIn || session.isLoggedIn()
    }

    fun go(destination: String, replaceRoot: Boolean = false) {
        if (!canOpen(destination)) {
            stack = listOf(RathYatraDestinations.SPLASH)
            return
        }
        stack = navigateStack(stack, destination, replaceRoot)
    }

    fun back() {
        stack = popStack(stack)
    }

    LaunchedEffect(Unit) {
        if (session.isLoggedIn() && stack == listOf(RathYatraDestinations.SPLASH)) {
            loggedIn = true
            stack = listOf(RathYatraDestinations.HOME)
        }
        if (current !in RathYatraDestinations.publicAccess && !loggedIn && !session.isLoggedIn()) {
            stack = listOf(RathYatraDestinations.SPLASH)
        }
    }

    // The server rejected the stored token (expired, revoked, or account disabled):
    // leave protected screens instead of continuing with cached data.
    val signedIn by session.signedIn
    LaunchedEffect(signedIn) {
        if (!signedIn && loggedIn && !openedOnProtectedScreen) {
            loggedIn = false
            if (current !in RathYatraDestinations.publicAccess) {
                stack = listOf(RathYatraDestinations.SPLASH, RathYatraDestinations.SIGN_IN)
            }
        }
    }

    if (stack.size > 1) {
        BackHandler(onBack = { back() })
    }

    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalCopy provides copy,
        LocalSetLanguage provides { languageKey = it.key },
    ) {
    Box(Modifier.fillMaxSize()) {
    when (current) {
        RathYatraDestinations.SPLASH -> SplashStartScreen(
            onGetStarted = { go(RathYatraDestinations.WELCOME) },
            onSignIn = { go(RathYatraDestinations.SIGN_IN) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.WELCOME,
        RathYatraDestinations.REGISTRATION_INTRO,
        -> WelcomeDiscoveryScreen(
            onGetStarted = { go(RathYatraDestinations.REGISTRATION_FORM) },
            onSignIn = { go(RathYatraDestinations.SIGN_IN) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.REGISTRATION_FORM -> RegistrationFormScreen(
            onBack = { back() },
            onContinue = {
                loggedIn = true
                go(RathYatraDestinations.REGISTRATION_SUCCESS)
            },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.REGISTRATION_SUCCESS -> RegistrationSuccessScreen(
            onGoHome = {
                loggedIn = true
                go(RathYatraDestinations.HOME, replaceRoot = true)
            },
            onViewPass = {
                loggedIn = true
                stack = listOf(RathYatraDestinations.HOME, RathYatraDestinations.QR_PASS)
            },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.HOME -> CitizenHomeScreen(
            onOpenTracking = { go(RathYatraDestinations.TRACKING) },
            onOpenQr = { go(RathYatraDestinations.QR_ATTENDANCE) },
            onOpenPass = { go(RathYatraDestinations.QR_PASS) },
            onOpenBenefits = { go(RathYatraDestinations.BENEFITS) },
            onOpenProfile = { go(RathYatraDestinations.PROFILE) },
            onOpenNotifications = { go(RathYatraDestinations.NOTIFICATIONS) },
            onSelectTab = { go(it) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.TRACKING -> ChariotTrackingScreen(
            onBack = { back() },
            onProfile = { go(RathYatraDestinations.PROFILE) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.QR_ATTENDANCE,
        RathYatraDestinations.QR_PASS,
        -> QrAttendanceScreen(
            showPassInitially = current == RathYatraDestinations.QR_PASS,
            onSelectTab = { go(it) },
            onOpenNotifications = { go(RathYatraDestinations.NOTIFICATIONS) },
            onOpenProfile = { go(RathYatraDestinations.PROFILE) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.SIGN_IN -> SignInScreen(
            onBack = { back() },
            onSignedIn = {
                loggedIn = true
                go(RathYatraDestinations.HOME, replaceRoot = true)
            },
            onCreateAccount = { go(RathYatraDestinations.REGISTRATION_FORM) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.NOTIFICATIONS -> DemoUnavailableScreen(
            title = copy.notificationsTitle,
            onBack = { back() },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.BENEFITS -> DemoUnavailableScreen(
            title = copy.benefitsTitle,
            onBack = { back() },
            showBottomBar = true,
            selectedTab = RathYatraDestinations.BENEFITS,
            onSelectTab = { go(it) },
            modifier = modifier.fillMaxSize(),
        )
        RathYatraDestinations.PROFILE -> ProfileScreen(
            session = session,
            onBack = { back() },
            onLogout = {
                scope.launch {
                    profileViewModel.logout()
                    loggedIn = false
                    stack = listOf(RathYatraDestinations.SPLASH)
                }
            },
            onOpenPass = { go(RathYatraDestinations.QR_PASS) },
            onSelectTab = { go(it) },
            modifier = modifier.fillMaxSize(),
        )
        else -> {
            LaunchedEffect(current) {
                stack = if (loggedIn || session.isLoggedIn()) {
                    listOf(RathYatraDestinations.HOME)
                } else {
                    listOf(RathYatraDestinations.SPLASH)
                }
            }
        }
    }
    ServerWakingBanner(
        visible = serverSlow,
        message = copy.serverWaking,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .windowInsetsPadding(WindowInsets.statusBars),
    )
    }
    }
}

/**
 * Shown while a request has been waiting more than a few seconds, typically while the
 * API host starts after being idle, so a slow first request does not look like a hang.
 */
@Composable
fun ServerWakingBanner(visible: Boolean, message: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            color = RathYatraColors.NavyDeep,
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = RathYatraColors.SaffronBright,
                )
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun RathYatraScreenPreview() {
    SahakarBharatiTheme(dynamicColor = false) {
        RathYatraScreen()
    }
}
