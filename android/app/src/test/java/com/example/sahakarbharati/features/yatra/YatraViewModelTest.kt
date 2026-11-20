package com.example.sahakarbharati.features.yatra

import com.example.sahakarbharati.data.remote.YatraApi
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.remote.YatraDistrictDto
import com.example.sahakarbharati.data.remote.YatraPageDto
import com.example.sahakarbharati.data.remote.YatraStopDto
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.repository.YatraCatalogPage
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
        val yatra = detail.state.yatra
        assertEquals(listOf(1, 2), yatra?.stops?.map { it.sequence })
        assertEquals("Nashik", yatra?.district?.name)
        assertEquals("YT00000001", yatra?.publicCode)
        assertEquals("09:30", yatraClock(yatra?.stops?.first()?.plannedArrival))
        assertEquals(null, yatraClock(yatra?.stops?.last()?.plannedArrival))
    }

    @Test
    fun unpublishedAndArchivedDetailAreNotShown() = runTest {
        val repository = RemoteYatraRepository(FakeYatraApi(), TokenSession())
        val detail = YatraDetailViewModel(repository)
        detail.load("draft-1")
        advanceUntilIdle()
        assertEquals(null, detail.state.yatra)
        assertEquals("This Yatra is not available.", detail.state.error)
        assertTrue(!detail.state.loading)

        detail.load("archived-1")
        advanceUntilIdle()
        assertEquals(null, detail.state.yatra)
        assertEquals("This Yatra is not available.", detail.state.error)
    }

    @Test
    fun staleListResponseAfterSessionChangeIsIgnored() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Result<YatraCatalogPage>>()
        val session = EpochSession()
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int) = gate.await()
            override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
        }
        val list = YatraListViewModel(repository, session)
        list.load()
        advanceUntilIdle()
        session.advance()
        gate.complete(Result.success(catalogPage(listOf(summary("published-1", true, "upcoming")))))
        advanceUntilIdle()
        assertTrue(list.state.items.isEmpty())
        assertEquals(null, list.state.error)
    }

    @Test
    fun olderDetailResponseDoesNotOverwriteTheCurrentYatra() = runTest {
        val first = kotlinx.coroutines.CompletableDeferred<Result<YatraDetailDto>>()
        val second = kotlinx.coroutines.CompletableDeferred<Result<YatraDetailDto>>()
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int) = Result.success(catalogPage(emptyList()))
            override suspend fun detail(yatraId: String) = if (yatraId == "a") first.await() else second.await()
        }
        val detail = YatraDetailViewModel(repository)
        detail.load("a")
        advanceUntilIdle()
        detail.load("b")
        advanceUntilIdle()
        second.complete(Result.success(sampleDetail("b")))
        advanceUntilIdle()
        assertEquals("b", detail.state.yatra?.id)
        first.complete(Result.success(sampleDetail("a")))
        advanceUntilIdle()
        assertEquals("b", detail.state.yatra?.id)
        assertEquals("b", detail.state.requestedId)
    }

    @Test
    fun retryReplacesAFailedCatalogAndDetailLoad() = runTest {
        var listCalls = 0
        var detailCalls = 0
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int): Result<YatraCatalogPage> {
                listCalls += 1
                return if (listCalls == 1) {
                    Result.failure(Exception("Yatras could not be loaded."))
                } else {
                    Result.success(catalogPage(listOf(summary("published-1", true, "upcoming"))))
                }
            }

            override suspend fun detail(yatraId: String): Result<YatraDetailDto> {
                detailCalls += 1
                return if (detailCalls == 1) {
                    Result.failure<YatraDetailDto>(Exception("This Yatra could not be loaded."))
                } else {
                    Result.success(sampleDetail(yatraId))
                }
            }
        }
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        assertEquals("Yatras could not be loaded.", list.state.error)
        assertTrue(list.state.items.isEmpty())
        list.load()
        advanceUntilIdle()
        assertEquals(listOf("published-1"), list.state.items.map { it.id })
        assertEquals(null, list.state.error)

        val viewModel = YatraDetailViewModel(repository)
        viewModel.load("published-1")
        advanceUntilIdle()
        assertEquals("This Yatra could not be loaded.", viewModel.state.error)
        viewModel.load("published-1")
        advanceUntilIdle()
        assertEquals("published-1", viewModel.state.yatra?.id)
        assertEquals(null, viewModel.state.error)
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
        published: Boolean?,
    ): YatraPageDto {
        assertEquals("Bearer token", authorization)
        assertEquals(true, published)
        return YatraPageDto(
            items = listOf(summary("published-1", true)),
            total = 1,
            limit = limit,
            offset = offset,
            hasMore = false,
        )
    }

    override suspend fun getYatra(authorization: String, yatraId: String): YatraDetailDto = sampleDetail(yatraId)
}

private class EpochSession : SessionStore {
    private var epoch = 0
    private val listeners = mutableListOf<(Int) -> Unit>()

    override fun isLoggedIn(): Boolean = true
    override fun accessToken(): String = "token"
    override fun cachedProfile() = null
    override fun saveSession(token: String, profile: com.example.sahakarbharati.data.model.ParticipantProfile) = Unit
    override fun saveProfile(profile: com.example.sahakarbharati.data.model.ParticipantProfile) = Unit
    override fun clearSession() = advance()
    override fun epoch(): Int = epoch
    override fun addEpochListener(listener: (Int) -> Unit) {
        listeners += listener
    }
    override fun removeEpochListener(listener: (Int) -> Unit) {
        listeners -= listener
    }

    fun advance() {
        epoch += 1
        listeners.toList().forEach { it(epoch) }
    }
}

private fun catalogPage(
    items: List<YatraSummaryDto>,
    offset: Int = 0,
    total: Int = items.size,
    hasMore: Boolean = false,
) = YatraCatalogPage(items, total, offset, hasMore)

private fun summary(id: String, published: Boolean, status: String = if (published) "upcoming" else "draft") =
    YatraSummaryDto(
        id = id,
        publicCode = "YT00000001",
        name = "Nashik Rath",
        district = YatraDistrictDto("district-1", "Nashik", "NSK"),
        startsAt = "2026-07-01T00:00:00Z",
        endsAt = "2026-07-12T00:00:00Z",
        status = status,
        published = published,
    )

private fun sampleDetail(yatraId: String) = YatraDetailDto(
    id = yatraId,
    publicCode = "YT00000001",
    name = "Nashik Rath",
    description = "A published Yatra.",
    district = YatraDistrictDto("district-1", "Nashik", "NSK"),
    startsAt = "2026-07-01T00:00:00Z",
    endsAt = "2026-07-12T00:00:00Z",
    status = if (yatraId == "archived-1") "archived" else if (yatraId == "draft-1") "draft" else "upcoming",
    published = yatraId != "draft-1",
    stops = listOf(
        YatraStopDto("stop-2", 2, "Second", "Road 2", true),
        YatraStopDto("stop-hidden", 3, "Closed", null, false),
        YatraStopDto(
            "stop-1",
            1,
            "First",
            "Road 1",
            true,
            plannedArrival = "2026-07-01T09:30:00Z",
        ),
    ),
)
