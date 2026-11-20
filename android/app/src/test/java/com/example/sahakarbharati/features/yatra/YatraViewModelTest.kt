package com.example.sahakarbharati.features.yatra

import com.example.sahakarbharati.data.remote.YatraApi
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.remote.YatraDistrictDto
import com.example.sahakarbharati.data.remote.YatraPageDto
import com.example.sahakarbharati.data.remote.YatraStopDto
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.repository.RemoteYatraRepository
import com.example.sahakarbharati.data.repository.SessionStore
import com.example.sahakarbharati.data.repository.YatraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YatraViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun publishedListDropsDraftsAndDetailOrdersActiveStops() = runTest {
        val api = FakeYatraApi()
        val repository = RemoteYatraRepository(api, TokenSession())
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        assertEquals(listOf("published-1"), list.state.items.map { it.id })
        assertEquals(null, list.state.error)

        val detail = YatraDetailViewModel(repository)
        detail.load("published-1")
        advanceUntilIdle()
        assertEquals(listOf(1, 2), detail.state.yatra?.stops?.map { it.sequence })
        assertEquals("Nashik", detail.state.yatra?.district?.name)
    }

    @Test
    fun unpublishedDetailIsNotShown() = runTest {
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int) = Result.success(
                YatraPageDto(items = emptyList(), total = 0, limit = 50, offset = offset, hasMore = false),
            )
            override suspend fun detail(yatraId: String) =
                Result.failure<YatraDetailDto>(Exception("This Yatra is not published."))
        }
        val detail = YatraDetailViewModel(repository)
        detail.load("draft-1")
        advanceUntilIdle()
        assertEquals(null, detail.state.yatra)
        assertEquals("This Yatra is not published.", detail.state.error)
        assertTrue(!detail.state.loading)
    }
}

private class TokenSession : SessionStore {
    override fun isLoggedIn(): Boolean = true
    override fun accessToken(): String = "token"
    override fun cachedProfile() = null
    override fun saveSession(token: String, profile: com.example.sahakarbharati.data.model.ParticipantProfile) = Unit
    override fun saveProfile(profile: com.example.sahakarbharati.data.model.ParticipantProfile) = Unit
    override fun clearSession() = Unit
}

private class FakeYatraApi : YatraApi {
    override suspend fun listYatras(
        authorization: String,
        limit: Int,
        offset: Int,
        published: Boolean,
    ): YatraPageDto {
        assertTrue(published)
        assertEquals("Bearer token", authorization)
        return YatraPageDto(
            items = listOf(summary("published-1", true), summary("draft-1", false)),
            total = 2,
            limit = limit,
            offset = offset,
        )
    }

    override suspend fun getYatra(authorization: String, yatraId: String): YatraDetailDto {
        return YatraDetailDto(
            id = yatraId,
            publicCode = "YT00000001",
            name = "Nashik Rath",
            description = "A published Yatra.",
            district = YatraDistrictDto("district-1", "Nashik", "NSK"),
            startsAt = "2026-07-01T00:00:00Z",
            endsAt = "2026-07-12T00:00:00Z",
            status = "upcoming",
            published = true,
            stops = listOf(
                YatraStopDto("stop-2", 2, "Second", "Road 2", true),
                YatraStopDto("stop-hidden", 3, "Closed", null, false),
                YatraStopDto("stop-1", 1, "First", "Road 1", true),
            ),
        )
    }

    private fun summary(id: String, published: Boolean) = YatraSummaryDto(
        id = id,
        publicCode = "YT00000001",
        name = "Nashik Rath",
        district = YatraDistrictDto("district-1", "Nashik", "NSK"),
        startsAt = "2026-07-01T00:00:00Z",
        endsAt = "2026-07-12T00:00:00Z",
        status = if (published) "upcoming" else "draft",
        published = published,
    )
}
