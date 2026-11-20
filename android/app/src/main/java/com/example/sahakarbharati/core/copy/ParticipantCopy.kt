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
    val editDetails: String,
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

fun participantCopy(language: AppLanguage): ParticipantCopy {
    fun s(key: String) = uiText(language, key)
    return ParticipantCopy(
        registeredParticipant = s("registeredParticipant"),
        account = s("account"),
        yourProfile = s("yourProfile"),
        profileStatus = s("profileStatus"),
        statusActive = s("statusActive"),
        statusDisabled = s("statusDisabled"),
        mobile = s("mobile"),
        mobileOnFile = s("mobileOnFile"),
        mobileOnFileBody = s("mobileOnFileBody"),
        email = s("email"),
        loginId = s("loginId"),
        fullName = s("fullName"),
        personalDetails = s("formTitle"),
        addressTitle = s("profileTitle"),
        address = s("address"),
        locality = s("locality"),
        district = s("district"),
        stateName = s("state"),
        pincode = s("pincode"),
        editDetails = s("editDetails"),
        saveChanges = s("saveChanges"),
        saving = s("saving"),
        saved = s("saved"),
        updateFailed = s("updateFailed"),
        loadFailed = s("loadFailed"),
        logOut = s("logOut"),
        logOutConfirm = s("logOutConfirm"),
        logOutBody = s("logOutBody"),
        cancel = s("cancel"),
        fullNameRequired = s("fullNameRequired"),
        pinDigits = s("pinDigits"),
    )
}

fun statusLabel(copy: ParticipantCopy, status: String?): String = when (status) {
    "disabled" -> copy.statusDisabled
    "active", null, "" -> copy.statusActive
    else -> status
}

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
