package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.QueuedFix
import com.example.sahakarbharati.data.remote.YatraApi
import com.example.sahakarbharati.data.remote.YatraAttendanceRecordDto
import com.example.sahakarbharati.data.remote.YatraAttendanceScanDto
import com.example.sahakarbharati.data.remote.YatraPositionCreateDto
import com.example.sahakarbharati.data.remote.mapApiError
import com.google.gson.JsonParser
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

sealed class PositionSubmit {
    data class Accepted(val status: Int) : PositionSubmit()
    data class Http(val status: Int, val message: String) : PositionSubmit()
    data object NoCredential : PositionSubmit()
}

interface YatraFieldRepository {
    suspend fun scan(yatraId: String, qrText: String): Result<YatraAttendanceRecordDto>
    suspend fun postPosition(fix: QueuedFix): PositionSubmit
}

class RemoteYatraFieldRepository(
    private val api: YatraApi,
    private val sessionStore: SessionStore,
    private val credentials: TrackerCredentialStore,
) : YatraFieldRepository {
    override suspend fun scan(yatraId: String, qrText: String): Result<YatraAttendanceRecordDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.scanAttendance("Bearer $token", yatraId, YatraAttendanceScanDto(qrText))
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Attendance could not be recorded."))
        }
    }

    override suspend fun postPosition(fix: QueuedFix): PositionSubmit {
        val token = credentials.trackerToken() ?: return PositionSubmit.NoCredential
        return try {
            val response = api.postPosition(
                "Bearer $token",
                fix.yatraId,
                YatraPositionCreateDto(
                    clientPositionId = fix.clientPositionId,
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyM = fix.accuracyM,
                    capturedAt = fix.capturedAt,
                ),
            )
            positionSubmit(response)
        } catch (error: IOException) {
            PositionSubmit.Http(0, mapApiError(error, "The server could not be reached. Check your connection."))
        } catch (error: HttpException) {
            PositionSubmit.Http(error.code(), mapApiError(error, "That position was not accepted."))
        }
    }

    private fun positionSubmit(response: Response<Unit>): PositionSubmit {
        val status = response.code()
        if (status == 200 || status == 201) return PositionSubmit.Accepted(status)
        return PositionSubmit.Http(status, responseDetail(response))
    }
}

private fun responseDetail(response: Response<*>): String {
    val raw = response.errorBody()?.string().orEmpty()
    if (raw.isBlank()) return "That position was not accepted."
    return runCatching {
        val detail = JsonParser.parseString(raw).asJsonObject.get("detail")
        when {
            detail == null -> "That position was not accepted."
            detail.isJsonPrimitive -> detail.asString
            else -> "That position was not accepted."
        }
    }.getOrDefault("That position was not accepted.")
}
