package com.example.sahakarbharati.data

/**
 * Yatra counter placards encode `SB-COUNTER:` plus an opaque token.
 * Booth placards (`SB-PLACE:`) are not accepted here.
 */
object CounterQr {
    private val payload = Regex("^SB-COUNTER:([A-Za-z0-9_-]{32,64})$")

    fun fromQr(raw: String): String? = payload.matchEntire(raw.trim())?.groupValues?.get(1)

    fun text(token: String): String = "SB-COUNTER:$token"
}
