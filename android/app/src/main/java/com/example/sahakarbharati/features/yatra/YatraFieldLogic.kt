package com.example.sahakarbharati.features.yatra

import com.example.sahakarbharati.data.remote.YatraAttendanceRecordDto

sealed class AttendanceOutcome {
    data object Submitting : AttendanceOutcome()
    data object Success : AttendanceOutcome()
    data object Duplicate : AttendanceOutcome()
    data class Invalid(val message: String) : AttendanceOutcome()
    data class Failed(val message: String) : AttendanceOutcome()
}

private val invalidDetails = setOf(
    "That counter was not found.",
    "This counter is not accepting attendance.",
    "This Yatra is not accepting attendance.",
    "That QR code is not a counter.",
)

fun attendanceOutcome(result: Result<YatraAttendanceRecordDto>): AttendanceOutcome {
    return result.fold(
        onSuccess = { record ->
            if (record.alreadyRecorded) AttendanceOutcome.Duplicate else AttendanceOutcome.Success
        },
        onFailure = { error ->
            val message = error.message?.trim().orEmpty().ifBlank { "Attendance could not be recorded." }
            if (message in invalidDetails) AttendanceOutcome.Invalid(message) else AttendanceOutcome.Failed(message)
        },
    )
}
