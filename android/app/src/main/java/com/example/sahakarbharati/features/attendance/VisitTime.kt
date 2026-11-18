package com.example.sahakarbharati.features.attendance

import com.example.sahakarbharati.core.copy.AppLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val india = ZoneId.of("Asia/Kolkata")

fun formatVisitTime(
    iso: String,
    fallback: String,
    language: AppLanguage = AppLanguage.EN,
): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return fallback
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.forLanguageTag(language.key))
    return formatter.format(instant.atZone(india))
}
