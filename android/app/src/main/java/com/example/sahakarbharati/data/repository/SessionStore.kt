package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.ParticipantProfile

interface SessionStore {
    fun isLoggedIn(): Boolean
    fun accessToken(): String?
    fun cachedProfile(): ParticipantProfile?
    fun saveSession(token: String, profile: ParticipantProfile)
    fun saveProfile(profile: ParticipantProfile)
    fun clearSession()

    /** Changes when the signed-in account is replaced or cleared. */
    fun epoch(): Int = 0

    fun addEpochListener(listener: (Int) -> Unit) = Unit

    fun removeEpochListener(listener: (Int) -> Unit) = Unit
}

/** Assignment credential for one Yatra tracker. Separate from the signed-in user token. */
interface TrackerCredentialStore {
    fun trackerToken(): String?
    fun saveTrackerToken(token: String)
    fun clearTrackerToken()
}

class MemoryTrackerCredentialStore : TrackerCredentialStore {
    private var stored: String? = null

    override fun trackerToken(): String? = stored

    override fun saveTrackerToken(token: String) {
        stored = token
    }

    override fun clearTrackerToken() {
        stored = null
    }
}

/** Session that never changes. ViewModel tests use it when account switching is not under test. */
object IdleSession : SessionStore {
    override fun isLoggedIn(): Boolean = false
    override fun accessToken(): String? = null
    override fun cachedProfile(): ParticipantProfile? = null
    override fun saveSession(token: String, profile: ParticipantProfile) = Unit
    override fun saveProfile(profile: ParticipantProfile) = Unit
    override fun clearSession() = Unit
}
