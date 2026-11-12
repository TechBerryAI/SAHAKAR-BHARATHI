package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.remote.AttendanceListDto
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.BoothCodeDto
import com.example.sahakarbharati.data.remote.CheckInRequest
import com.example.sahakarbharati.data.remote.SahakarApi
import com.example.sahakarbharati.data.remote.mapApiError

interface AttendanceRepository {
    suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto>
    suspend fun mine(): Result<AttendanceListDto>
    suspend fun boothCodes(): Result<List<BoothCodeDto>>
}

class RemoteAttendanceRepository(
    private val api: SahakarApi,
    private val sessionStore: SessionStore,
) : AttendanceRepository {
    override suspend fun checkIn(code: String, source: String): Result<AttendanceRecordDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.checkIn("Bearer $token", CheckInRequest(code = code, source = source))
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Attendance could not be recorded."))
        }
    }

    override suspend fun mine(): Result<AttendanceListDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.myAttendance("Bearer $token")
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Check-in history could not be loaded."))
        }
    }

    override suspend fun boothCodes(): Result<List<BoothCodeDto>> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.listBoothCodes("Bearer $token").items
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Booth codes could not be loaded."))
        }
    }
}
