package com.example.sahakarbharati.data

/**
 * Booth placards encode `SB-PLACE:` plus the 6-digit code. Anything else is ignored.
 */
object BoothCode {
    private val payload = Regex("^SB-PLACE:(\\d{6})$")

    fun fromQr(raw: String): String? = payload.matchEntire(raw.trim())?.groupValues?.get(1)

    fun fromManual(raw: String): String? {
        val digits = raw.trim()
        return digits.takeIf { it.length == 6 && it.all(Char::isDigit) }
    }

    fun text(code: String): String = "SB-PLACE:$code"
}
