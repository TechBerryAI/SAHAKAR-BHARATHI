package com.example.sahakarbharati.data.repository

import com.example.sahakarbharati.data.remote.YatraApi
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.remote.mapApiError

data class YatraCatalogPage(
    val items: List<YatraSummaryDto>,
    val total: Int,
    val offset: Int,
    val hasMore: Boolean,
    val received: Int = items.size,
)

interface YatraRepository {
    suspend fun published(offset: Int = 0): Result<YatraCatalogPage>
    suspend fun detail(yatraId: String): Result<YatraDetailDto>
}

class RemoteYatraRepository(
    private val api: YatraApi,
    private val sessionStore: SessionStore,
) : YatraRepository {
    override suspend fun published(offset: Int): Result<YatraCatalogPage> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        val fallback = if (offset == 0) "Yatras could not be loaded." else "Could not load more Yatras."
        return runCatching {
            val page = api.listYatras("Bearer $token", limit = PAGE_SIZE, offset = offset, published = true)
            YatraCatalogPage(
                items = page.items.filter { it.published && it.status != "archived" },
                total = page.total,
                offset = page.offset,
                hasMore = page.hasMore,
                received = page.items.size,
            )
        }.recoverCatching { error ->
            throw Exception(mapApiError(error, fallback))
        }
    }

    override suspend fun detail(yatraId: String): Result<YatraDetailDto> {
        val token = sessionStore.accessToken()
            ?: return Result.failure(Exception("Sign in is required."))
        return runCatching {
            val detail = api.getYatra("Bearer $token", yatraId)
            if (!detail.published || detail.status == "archived") {
                throw Exception("This Yatra is not available.")
            }
            detail.copy(stops = detail.stops.filter { it.active }.sortedBy { it.sequence })
        }.recoverCatching { error ->
            if (error is Exception && error.message == "This Yatra is not available.") throw error
            throw Exception(mapApiError(error, "This Yatra could not be loaded."))
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
