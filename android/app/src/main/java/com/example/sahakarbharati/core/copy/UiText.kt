package com.example.sahakarbharati.core.copy

import androidx.compose.runtime.Composable

fun uiText(
    language: AppLanguage,
    key: String,
    values: Map<String, String> = emptyMap(),
): String {
    var text = uiPack(language)[key] ?: uiPack(AppLanguage.EN)[key] ?: key
    values.forEach { (name, value) ->
        text = text.replace("{$name}", value)
    }
    return text
}

fun uiCount(language: AppLanguage, count: Int, oneKey: String, manyKey: String): String =
    uiText(language, if (count == 1) oneKey else manyKey, mapOf("count" to count.toString()))

@Composable
fun ui(key: String, values: Map<String, String> = emptyMap()): String =
    uiText(LocalLanguage.current, key, values)

@Composable
fun uiKnown(message: String): String = localizeKnown(LocalLanguage.current, message)

private val knownEnglish = mapOf(
    "An account with these details already exists." to "duplicateAccount",
    "An account already exists for this mobile number." to "mobileRegistered",
    "An account already exists with this email address." to "emailRegistered",
    "Mobile number or PIN is incorrect." to "invalidCredentials",
    "Too many attempts. Try again later." to "tooManyAttempts",
    "That booth was not found." to "boothNotFound",
    "This booth is not accepting check-ins." to "boothInactive",
    "Enter the 6-digit booth code." to "boothCodeInvalid",
    "The code is incorrect or has expired." to "otpInvalid",
    "Verify this mobile number before registering." to "verificationExpired",
    "The server could not be reached. Check your connection." to "networkError",
    "Could not send the OTP." to "otpSendFailed",
    "Could not verify the OTP." to "otpVerifyFailed",
    "Registration failed." to "registrationFailed",
    "Sign in failed." to "signInFailed",
    "Profile could not be loaded." to "loadFailed",
    "Profile could not be updated." to "updateFailed",
    "Could not load registered users." to "couldNotLoadUsers",
    "Could not load this person." to "couldNotLoadPerson",
    "Could not update access." to "couldNotUpdateAccess",
    "Could not load booths." to "couldNotLoadBooths",
    "Could not load this booth." to "couldNotLoadBooth",
    "Could not create this booth." to "couldNotCreateBooth",
    "Could not update this booth." to "couldNotUpdateBooth",
    "Could not load attendance." to "couldNotLoadAttendance",
    "Attendance could not be recorded." to "attendanceFailed",
    "Check-in history could not be loaded." to "historyFailed",
    "Booth codes could not be loaded." to "codesFailed",
    "Booth name is required." to "boothNameRequired",
    "String should have at least 2 characters" to "tooShort",
    "please try again." to "tryAgain",
)

private val createdAccount = Regex(
    """^Your account was created for mobile (.+?), but signing in failed: (.+) Sign in with that mobile number and PIN\.$""",
)

/** The API speaks English. Known sentences are shown in the selected language. */
fun localizeKnown(language: AppLanguage, message: String): String {
    val trimmed = message.trim()
    knownEnglish[trimmed]?.let { return uiText(language, it) }
    val created = createdAccount.matchEntire(trimmed) ?: return message
    return uiText(
        language,
        "accountCreated",
        mapOf(
            "mobile" to created.groupValues[1],
            "detail" to localizeKnown(language, created.groupValues[2].trim()),
        ),
    )
}

fun genderLabel(language: AppLanguage, gender: String?): String {
    val raw = gender?.trim().orEmpty()
    if (raw.isEmpty()) return "—"
    return when (raw.lowercase()) {
        "male" -> uiText(language, "male")
        "female" -> uiText(language, "female")
        "other" -> uiText(language, "other")
        else -> titleCase(raw)
    }
}

fun roleLabel(language: AppLanguage, role: String): String = when (role) {
    "super_admin" -> uiText(language, "superAdmin")
    "admin" -> uiText(language, "adminRole")
    else -> uiText(language, "participant")
}

internal fun applySharedCopy(copy: RathYatraCopy, language: AppLanguage) {
    fun s(key: String) = uiText(language, key)
    copy.title = s("brandName")
    copy.honourLine = s("honour")
    copy.subtitle = s("subtitle")
    copy.getStarted = s("registerNow")
    copy.signIn = s("signIn")
    copy.signInOfficial = s("alreadyRegistered")
    copy.greeting = s("jaiKisan")
    copy.tabHome = s("home")
    copy.tabProfile = s("profile")
    copy.portalChip = s("govChip")
    copy.topBarKicker = s("govChip")
    copy.legalName = s("legalName")
    copy.nameHint = s("nameAadhaarHint")
    copy.gender = s("gender")
    copy.male = s("male")
    copy.female = s("female")
    copy.other = s("other")
    copy.farmer = s("farmer")
    copy.fisherman = s("fisherman")
    copy.both = s("both")
    copy.allied = s("allied")
    copy.occupation = s("occupation")
    copy.occupationHint = s("occupationHint")
    copy.dob = s("dobFull")
    copy.ageMin = s("ageMin")
    copy.formTitle = s("formTitle")
    copy.govtPortal = s("govtPortal")
    copy.createAccount = s("createAccountBtn")
    copy.sosTitle = s("sosTitle")
    copy.sosBody = s("sosBody")
    copy.chooseLanguage = s("chooseLanguage")
    copy.searchLanguages = s("searchLanguages")
    copy.participantId = s("loginId")
    copy.goHome = s("goHome")
    copy.serverWaking = s("serverWaking")
    copy.signInTitle = s("signIn")
    copy.scanTab = s("scanBoothTitle")
    copy.pinBody = s("scanBoothBody")
    copy.attendanceTitle = s("checkInTitle")
    copy.scannerActive = s("pointAtQr")
    copy.pinLabel = s("pinField")
    copy.submitPin = s("markAttendance")
    copy.historyTitle = s("checkInHistory")
    copy.noVisitsYet = s("noVisitsYet")
    copy.markedHere = s("markedHere")
    copy.alreadyHere = s("alreadyHere")
    copy.recordedDetails = s("recordedAs")
    copy.allowCamera = s("allowCamera")
    copy.cameraOff = s("cameraOff")
    copy.scanQr = s("scanQr")
    copy.scanMiss = s("scanMiss")
    copy.scanStarting = s("scanStarting")
}
