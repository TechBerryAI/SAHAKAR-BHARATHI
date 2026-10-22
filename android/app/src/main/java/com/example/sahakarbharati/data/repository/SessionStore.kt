package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.model.ParticipantProfile

interface SessionStore {
    fun isLoggedIn(): Boolean
    fun accessToken(): String?
    fun cachedProfile(): ParticipantProfile?
    fun saveSession(token: String, profile: ParticipantProfile)
    fun saveProfile(profile: ParticipantProfile)
    fun clearSession()
}
