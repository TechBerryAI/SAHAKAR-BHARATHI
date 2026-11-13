package com.example.sahakarbharati.features.attendance

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val visitTime = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
private val india = ZoneId.of("Asia/Kolkata")

fun formatVisitTime(iso: String, fallback: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return fallback
    return visitTime.format(instant.atZone(india))
}
