package com.example.sahakarbharati.data.remote

import com.example.sahakarbharati.data.AppSession
import okhttp3.Interceptor
import okhttp3.Response

private const val BEARER_PREFIX = "Bearer "

/** A tracker assignment uses a different bearer, so its 401 must leave the signed-in user alone. */
fun bearerMatchesStored(authorization: String?, storedUserToken: String?): Boolean {
    if (authorization == null || storedUserToken.isNullOrBlank()) return false
    if (!authorization.startsWith(BEARER_PREFIX)) return false
    return authorization.removePrefix(BEARER_PREFIX) == storedUserToken
}

/**
 * The server answers 401 when a session is expired, revoked, or belongs to a disabled
 * account. Any such answer to an authenticated call ends the local session so the
 * app returns to sign-in instead of showing cached data as if still signed in.
 */
class SessionExpiryInterceptor(private val session: AppSession) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val authorization = request.header("Authorization")
        if (response.code == 401 && bearerMatchesStored(authorization, session.accessToken())) {
            session.clearIfCurrent(authorization!!.removePrefix(BEARER_PREFIX))
        }
        return response
    }
}
