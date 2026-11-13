package com.example.sahakarbharati.features.registration

import java.util.Calendar
import java.util.TimeZone

fun parseDobToUtcMillis(dob: String): Long? {
    val parts = dob.split("/").map { it.trim() }
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.clear()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    return calendar.timeInMillis
}

fun isIndianMobile(mobile: String): Boolean =
    mobile.matches(Regex("^[6-9]\\d{9}$"))

fun isoDob(dob: String): String? {
    val parts = dob.split("/").map { it.trim() }
    if (parts.size != 3) return null
    val day = parts[0]
    val month = parts[1]
    val year = parts[2]
    if (day.length != 2 || month.length != 2 || year.length != 4) return null
    return "$year-$month-$day"
}

fun formatDob(utcMillis: Long): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = utcMillis
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return "%02d / %02d / %04d".format(day, month, year)
}

fun ageYearsFromDob(dob: String): Int {
    val birth = parseDobToUtcMillis(dob) ?: return 0
    val born = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    born.timeInMillis = birth
    val today = Calendar.getInstance()
    var years = today.get(Calendar.YEAR) - born.get(Calendar.YEAR)
    val birthdayPassed = today.get(Calendar.MONTH) > born.get(Calendar.MONTH) ||
        (
            today.get(Calendar.MONTH) == born.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) >= born.get(Calendar.DAY_OF_MONTH)
            )
    if (!birthdayPassed) years -= 1
    return years.coerceAtLeast(0)
}

fun joinDob(day: String, month: String, year: String): String = "$day/$month/$year"

fun adultDob(dob: String): Boolean {
    val parts = dob.split("/")
    if (parts.size != 3) return false
    val day = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    if (month !in 1..12) return false
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    if (day !in 1..calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) return false
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - year
    val todayMonth = today.get(Calendar.MONTH) + 1
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    if (todayMonth < month || (todayMonth == month && todayDay < day)) age -= 1
    return age >= 18
}
