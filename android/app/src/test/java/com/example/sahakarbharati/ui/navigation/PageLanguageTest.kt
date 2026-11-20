package com.example.sahakarbharati.ui.navigation

import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.copyFor
import com.example.sahakarbharati.core.copy.participantCopy
import com.example.sahakarbharati.core.copy.uiText
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks every visible phrase on every screen, for every language in the picker.
 * A phrase passes when it is present and, outside English, is not the English sentence.
 * PIN, OTP, SOS, QR, OK, and Email may stay as those short international words.
 */
class PageLanguageTest {

    @Test
    fun everyPageIsTranslatedInEveryLanguage() {
        val failures = mutableListOf<String>()
        val report = StringBuilder()
        report.appendLine("Language | " + pages.keys.joinToString(" | "))
        AppLanguage.entries.forEach { language ->
            val cells = pages.map { (page, keys) ->
                val missed = keys.filter { key -> !passes(language, key) }
                if (missed.isNotEmpty()) {
                    failures += "${language.englishName} / $page / " + missed.joinToString(", ")
                }
                if (missed.isEmpty()) "PASS ${keys.size}/${keys.size}" else "FAIL ${keys.size - missed.size}/${keys.size}"
            }
            report.appendLine("${language.englishName} | " + cells.joinToString(" | "))
            val months = uiText(language, "monthNames").split(Regex("\\s+")).filter { it.isNotBlank() }
            if (months.size != 12) {
                failures += "${language.englishName} / Registration / monthNames has ${months.size} months"
            }
        }
        screensReadTheTranslatedCopy(failures)
        val text = report.toString().trimEnd()
        println(text)
        File("build").mkdirs()
        File("build/page-language-report.txt").writeText(text)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun passes(language: AppLanguage, key: String): Boolean {
        val value = uiText(language, key).trim()
        if (value.isEmpty() || value == key) return false
        if (language == AppLanguage.EN) return true
        val english = uiText(AppLanguage.EN, key).trim()
        return value != english || english in keptWords
    }

    private fun screensReadTheTranslatedCopy(failures: MutableList<String>) {
        AppLanguage.entries.filter { it != AppLanguage.EN }.forEach { language ->
            val copy = copyFor(language)
            val profile = participantCopy(language)
            checkWired(failures, language, "Welcome", copy.getStarted, "registerNow")
            checkWired(failures, language, "Welcome", copy.subtitle, "subtitle")
            checkWired(failures, language, "Sign in", copy.signIn, "signIn")
            checkWired(failures, language, "Home", copy.tabHome, "home")
            checkWired(failures, language, "Home", copy.greeting, "jaiKisan")
            checkWired(failures, language, "Home", copy.scanTab, "scanBoothTitle")
            checkWired(failures, language, "Profile", copy.tabProfile, "profile")
            checkWired(failures, language, "Profile", profile.logOut, "logOut")
            checkWired(failures, language, "Profile", profile.registeredParticipant, "registeredParticipant")
            checkWired(failures, language, "Check-in", copy.attendanceTitle, "checkInTitle")
            checkWired(failures, language, "Registration", copy.createAccount, "createAccountBtn")
            checkWired(failures, language, "Registration", copy.farmer, "farmer")
        }
    }

    private fun checkWired(
        failures: MutableList<String>,
        language: AppLanguage,
        page: String,
        shown: String,
        key: String,
    ) {
        val expected = uiText(language, key)
        if (shown != expected) {
            failures += "${language.englishName} / $page shows '$shown' instead of '$expected'"
        }
    }

    private val keptWords = setOf("PIN", "OTP", "SOS", "QR", "OK", "Email")

    private val pages = linkedMapOf(
        "Welcome" to listOf(
            "honour", "welcomeTitle", "subtitle", "registerNow", "alreadyRegistered",
        ),
        "Sign in" to listOf(
            "signIn", "enterLoginPin", "accessTitle", "accessBody", "accountReady",
            "loginIdLabel", "loginIdHint", "pinLabel", "showSecret", "hideSecret",
        ),
        "Verify mobile" to listOf(
            "verifyMobile", "mobileLabel", "mobileHint", "otpLabel", "otpSmsHint",
            "resendOtp", "signIn", "pleaseWait", "continue", "verifyOtp", "sendOtp",
            "otpSent", "otpSendFirst", "otpVerified", "otpRequired", "otpSendFailed",
            "otpVerifyFailed", "otpInvalid", "otpVerifyRequired",
        ),
        "Registration" to listOf(
            "registrationTitle", "formProgress", "stepPersonalTrack", "stepContactTrack",
            "stepProfileTrack", "stepSecurityTrack", "govtPortal", "formTitle", "legalName",
            "nameAadhaarHint", "gender", "male", "female", "other", "dobFull", "dobDay",
            "dobMonth", "dobYear", "monthNames", "ageMin", "occupation", "occupationHint",
            "occupationSelect", "farmer", "fisherman", "both", "allied", "contactTitle",
            "contactVerifiedHint", "emailOptional", "contactEmailHint", "profileTitle",
            "addressCardBody", "houseLabel", "localityLabel", "districtLabel", "stateLabel",
            "pincodeLabel", "securityTitle", "securityCardTitle", "securityCardBody",
            "securityPinLabel", "securityPinHint", "securityConfirmPin", "continue", "createAccountBtn",
            "fullNameRequired", "genderRequired", "dobRequired", "minAge", "occupationRequired",
            "addressRequired", "passwordLength", "passwordMatch", "ok", "cancel",
        ),
        "Account ready" to listOf(
            "registered", "accountReady", "keepLoginId", "keepLoginIdBody", "loginId", "goHome",
        ),
        "Home" to listOf(
            "brandName", "registeredParticipant", "jaiKisan", "mobile", "mobileOnFileBody",
            "scanBoothTitle", "scanBoothBody", "home", "profile", "adminRole",
        ),
        "Profile" to listOf(
            "profile", "registeredParticipant", "formTitle", "fullName", "farmer", "fisherman",
            "both", "allied", "profileTitle", "address", "locality", "district", "state",
            "pincode", "editDetails", "saveChanges", "saving", "saved", "account", "loginId", "mobile", "email",
            "logOut", "logOutConfirm", "logOutBody", "cancel", "fullNameRequired", "pinDigits",
        ),
        "Check-in" to listOf(
            "checkInTitle", "pointAtQr", "scanBanner", "scanBoothBody", "pinField",
            "markAttendance", "checkInHistory", "noVisitsYet", "markedHere", "alreadyHere",
            "recordedAs", "cameraOff", "allowCamera", "scanMiss", "scanQr", "scanStarting",
        ),
        "Registered users" to listOf(
            "registeredUsers", "people", "booths", "searchPeople", "loading", "onePerson",
            "manyPeople", "noSearchMatch", "searchTry", "pleaseWait", "loadMore", "participant",
            "adminRole", "superAdmin",
        ),
        "User details" to listOf(
            "userDetails", "account", "loginId", "mobile", "email", "gender", "male", "female",
            "other", "occupation", "farmer", "address", "locality", "district", "state",
            "pincode", "adminAccessOn", "participantOnly", "adminAccessOnBody", "giveAdminBody",
            "removeAdmin", "giveAdmin",
        ),
        "Booths" to listOf(
            "booths", "people", "boothName", "boothExample", "createBooth", "pleaseWait",
            "noBooths", "noBoothsBody", "acceptingVisits", "notAccepting", "boothNameRequired",
        ),
        "Booth detail" to listOf(
            "boothFallback", "boothQrDesc", "qrFailed", "scanOrEnter", "printQr", "stopVisits",
            "acceptAgain", "whoWasHere", "oneVisit", "manyVisits", "noCheckIns", "loadMore",
        ),
        "Language list" to listOf(
            "chooseLanguage", "searchLanguages", "noLanguages",
        ),
        "Help and errors" to listOf(
            "sos", "sosTitle", "sosBody", "close", "navigateBack", "networkError", "serverWaking",
            "signInFailed", "registrationFailed", "loadFailed", "updateFailed", "otpSendFailed",
            "otpVerifyFailed", "invalidCredentials", "mobileRegistered", "boothNotFound",
            "boothInactive", "tooShort",
        ),
    )
}
