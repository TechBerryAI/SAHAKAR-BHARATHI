package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.remote.YatraApi
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.remote.mapApiError

interface YatraRepository {
    suspend fun published(): Result<List<YatraSummaryDto>>
    suspend fun detail(yatraId: String): Result<YatraDetailDto>
}

class RemoteYatraRepository(
    private val api: YatraApi,
    private val sessionStore: SessionStore,
) : YatraRepository {
    override suspend fun published(): Result<List<YatraSummaryDto>> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            api.listYatras("Bearer $token", limit = 50, offset = 0)
                .items
                .filter { it.published }
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, "Yatras could not be loaded."))
        }
    }

    override suspend fun detail(yatraId: String): Result<YatraDetailDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            val detail = api.getYatra("Bearer $token", yatraId)
            if (!detail.published) throw Exception("This Yatra is not published.")
            detail.copy(stops = detail.stops.filter { it.active }.sortedBy { it.sequence })
        }.recoverCatching { error ->
            if (error is Exception && error.message == "This Yatra is not published.") throw error
            throw Exception(mapApiError(error, "This Yatra could not be loaded."))
        }
    }
}
