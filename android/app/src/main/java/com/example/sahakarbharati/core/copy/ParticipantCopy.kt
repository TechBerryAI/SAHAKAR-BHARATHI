package com.example.sahakarbharati.core.copy

data class ParticipantCopy(
    val registeredParticipant: String,
    val account: String,
    val yourProfile: String,
    val profileStatus: String,
    val statusActive: String,
    val statusDisabled: String,
    val mobile: String,
    val mobileOnFile: String,
    val mobileOnFileBody: String,
    val email: String,
    val loginId: String,
    val fullName: String,
    val personalDetails: String,
    val addressTitle: String,
    val address: String,
    val locality: String,
    val district: String,
    val stateName: String,
    val pincode: String,
    val saveChanges: String,
    val saving: String,
    val saved: String,
    val updateFailed: String,
    val loadFailed: String,
    val logOut: String,
    val logOutConfirm: String,
    val logOutBody: String,
    val cancel: String,
    val fullNameRequired: String,
    val pinDigits: String,
)

fun participantCopy(language: AppLanguage): ParticipantCopy =
    if (AppLanguage.usesHindiCopy(language)) hindiParticipant else englishParticipant

fun statusLabel(copy: ParticipantCopy, status: String?): String = when (status) {
    "disabled" -> copy.statusDisabled
    "active", null, "" -> copy.statusActive
    else -> status
}

private val englishParticipant = ParticipantCopy(
    registeredParticipant = "Registered participant",
    account = "Account",
    yourProfile = "Your Rath Yatra profile",
    profileStatus = "Status: {status}. Sign in with your mobile number and PIN, and update your details from Profile.",
    statusActive = "Active",
    statusDisabled = "Disabled",
    mobile = "Mobile",
    mobileOnFile = "Mobile on file",
    mobileOnFileBody = "This number is on file for your Rath Yatra profile.",
    email = "Email",
    loginId = "Participant ID",
    fullName = "Full name",
    personalDetails = "Personal Details",
    addressTitle = "Address",
    address = "Address",
    locality = "Locality",
    district = "District",
    stateName = "State",
    pincode = "PIN code",
    saveChanges = "Save changes",
    saving = "Saving…",
    saved = "Profile updated.",
    updateFailed = "Could not update profile.",
    loadFailed = "Could not load profile.",
    logOut = "Log out",
    logOutConfirm = "Log out?",
    logOutBody = "You will need your mobile number and PIN to open the app again.",
    cancel = "Cancel",
    fullNameRequired = "Full name is required.",
    pinDigits = "PIN code must be 6 digits.",
)

private val hindiParticipant = ParticipantCopy(
    registeredParticipant = "पंजीकृत प्रतिभागी",
    account = "खाता",
    yourProfile = "आपकी रथ यात्रा प्रोफ़ाइल",
    profileStatus = "स्थिति: {status}। साइन इन मोबाइल नंबर और पिन से करें। विवरण प्रोफ़ाइल से बदलें।",
    statusActive = "सक्रिय",
    statusDisabled = "निष्क्रिय",
    mobile = "मोबाइल",
    mobileOnFile = "दर्ज मोबाइल नंबर",
    mobileOnFileBody = "यह नंबर आपकी रथ यात्रा प्रोफ़ाइल पर दर्ज है।",
    email = "ईमेल",
    loginId = "प्रतिभागी आईडी",
    fullName = "पूरा नाम",
    personalDetails = "व्यक्तिगत विवरण",
    addressTitle = "पता",
    address = "पता",
    locality = "इलाका",
    district = "ज़िला",
    stateName = "राज्य",
    pincode = "पिन कोड",
    saveChanges = "बदलाव सहेजें",
    saving = "सहेजा जा रहा है…",
    saved = "प्रोफ़ाइल अपडेट हो गई।",
    updateFailed = "प्रोफ़ाइल अपडेट नहीं हो सकी।",
    loadFailed = "प्रोफ़ाइल नहीं खुल सकी।",
    logOut = "लॉग आउट",
    logOutConfirm = "लॉग आउट करें?",
    logOutBody = "ऐप फिर खोलने के लिए मोबाइल नंबर और पिन चाहिए।",
    cancel = "रद्द करें",
    fullNameRequired = "पूरा नाम आवश्यक है।",
    pinDigits = "पिन कोड 6 अंकों का होना चाहिए।",
)

fun formatMobile(mobile: String): String = if (mobile.length == 10) "+91 $mobile" else mobile.ifBlank { "—" }

fun personInitials(name: String): String {
    val letters = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
    return letters.ifBlank { "SB" }
}

fun regionLabel(district: String?, state: String?): String =
    listOfNotNull(district?.takeIf { it.isNotBlank() }, state?.takeIf { it.isNotBlank() }).joinToString(", ")

fun titleCase(value: String): String = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }

fun blankToNull(value: String): String? = value.trim().ifBlank { null }
