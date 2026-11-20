package com.example.sahakarbharati.features.yatra

import com.example.sahakarbharati.data.remote.YatraApi
import com.example.sahakarbharati.data.remote.YatraDetailDto
import com.example.sahakarbharati.data.remote.YatraDistrictDto
import com.example.sahakarbharati.data.remote.YatraPageDto
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.repository.RemoteYatraRepository
import com.example.sahakarbharati.data.repository.SessionStore
import com.example.sahakarbharati.data.repository.YatraCatalogPage
import com.example.sahakarbharati.data.repository.YatraRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YatraCatalogPagingTest {
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
    fun firstPageReportsThatAnotherPublicPageExists() = runTest {
        val repository = pagingRepository(first = page(rows(50), total = 75, hasMore = true))
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        assertEquals(50, list.state.items.size)
        assertTrue(list.state.hasMore)
        assertEquals(null, list.state.error)
    }

    @Test
    fun secondPageAppendsWithoutDroppingOrDuplicatingTheFirst() = runTest {
        val first = rows(50, prefix = "a")
        val second = listOf(first.last()) + rows(25, prefix = "b")
        val repository = pagingRepository(
            first = page(first, total = 75, hasMore = true),
            next = page(second, offset = 50, total = 75, hasMore = false),
        )
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        list.loadMore()
        advanceUntilIdle()
        assertEquals(first.map { it.id } + second.drop(1).map { it.id }, list.state.items.map { it.id })
        assertEquals(75, list.state.items.size)
        assertFalse(list.state.hasMore)
    }

    @Test
    fun publishedRequestSkipsDraftsThatWouldHaveFilledTheFirstRawPage() = runTest {
        val api = object : YatraApi {
            override suspend fun listYatras(
                authorization: String,
                limit: Int,
                offset: Int,
                published: Boolean?,
            ): YatraPageDto {
                assertEquals(50, limit)
                assertEquals(0, offset)
                if (published != true) {
                    return YatraPageDto(rows(50, prefix = "draft", published = false), 51, limit, offset, true)
                }
                return YatraPageDto(listOf(yatra("beyond-drafts")), 1, limit, offset, false)
            }

            override suspend fun getYatra(authorization: String, yatraId: String) = error("unused")
        }
        val page = RemoteYatraRepository(api, SignedInSession()).published(0).getOrThrow()
        assertEquals(listOf("beyond-drafts"), page.items.map { it.id })
        assertFalse(page.hasMore)
    }

    @Test
    fun lastPageStopsFurtherRequests() = runTest {
        var calls = 0
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int): Result<YatraCatalogPage> {
                calls += 1
                return if (offset == 0) {
                    Result.success(page(rows(1, prefix = "first"), total = 2, hasMore = true))
                } else {
                    Result.success(page(rows(1, prefix = "last"), offset = 1, total = 2, hasMore = false))
                }
            }

            override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
        }
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        assertTrue(list.state.hasMore)
        list.loadMore()
        advanceUntilIdle()
        assertFalse(list.state.hasMore)
        list.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(listOf("first-1", "last-1"), list.state.items.map { it.id })
    }

    @Test
    fun repeatedLoadMoreStartsOnlyOneRequest() = runTest {
        val gate = CompletableDeferred<Result<YatraCatalogPage>>()
        var nextCalls = 0
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int): Result<YatraCatalogPage> {
                if (offset == 0) return Result.success(page(rows(1), total = 2, hasMore = true))
                nextCalls += 1
                return gate.await()
            }

            override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
        }
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        list.loadMore()
        list.loadMore()
        list.loadMore()
        advanceUntilIdle()
        assertEquals(1, nextCalls)
        gate.complete(Result.success(page(rows(1, prefix = "next"), offset = 1, total = 2, hasMore = false)))
        advanceUntilIdle()
        assertEquals(listOf("page-1", "next-1"), list.state.items.map { it.id })
    }

    @Test
    fun failedNextPageKeepsTheFirstPageUntilRetry() = runTest {
        var calls = 0
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int): Result<YatraCatalogPage> {
                calls += 1
                return when {
                    offset == 0 -> Result.success(page(rows(2, prefix = "kept"), total = 3, hasMore = true))
                    calls == 2 -> Result.failure(Exception("Could not load more Yatras."))
                    else -> Result.success(page(rows(1, prefix = "added"), offset = 2, total = 3, hasMore = false))
                }
            }

            override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
        }
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        list.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("kept-1", "kept-2"), list.state.items.map { it.id })
        assertEquals("Could not load more Yatras.", list.state.nextPageError)
        assertTrue(list.state.hasMore)
        list.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("kept-1", "kept-2", "added-1"), list.state.items.map { it.id })
        assertEquals(null, list.state.nextPageError)
        assertFalse(list.state.hasMore)
    }

    @Test
    fun pageResponseAfterSessionChangeIsIgnored() = runTest {
        val gate = CompletableDeferred<Result<YatraCatalogPage>>()
        val session = CatalogEpochSession()
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int): Result<YatraCatalogPage> {
                if (offset == 0) return Result.success(page(rows(1, prefix = "mine"), total = 2, hasMore = true))
                return gate.await()
            }

            override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
        }
        val list = YatraListViewModel(repository, session)
        list.load()
        advanceUntilIdle()
        list.loadMore()
        advanceUntilIdle()
        session.advance()
        gate.complete(Result.success(page(rows(1, prefix = "old"), offset = 1, total = 2, hasMore = false)))
        advanceUntilIdle()
        assertTrue(list.state.items.isEmpty())
        assertEquals(null, list.state.nextPageError)
    }

    @Test
    fun olderCatalogResponseCannotReplaceTheLatestPage() = runTest {
        val first = CompletableDeferred<Result<YatraCatalogPage>>()
        val second = CompletableDeferred<Result<YatraCatalogPage>>()
        var calls = 0
        val repository = object : YatraRepository {
            override suspend fun published(offset: Int): Result<YatraCatalogPage> {
                calls += 1
                return if (calls == 1) first.await() else second.await()
            }

            override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
        }
        val list = YatraListViewModel(repository)
        list.load()
        advanceUntilIdle()
        list.load()
        advanceUntilIdle()
        second.complete(Result.success(page(listOf(yatra("latest")))))
        advanceUntilIdle()
        first.complete(Result.success(page(listOf(yatra("stale")))))
        advanceUntilIdle()
        assertEquals(listOf("latest"), list.state.items.map { it.id })
    }
}

private fun pagingRepository(first: YatraCatalogPage, next: YatraCatalogPage = first) = object : YatraRepository {
    override suspend fun published(offset: Int) = Result.success(if (offset == 0) first else next)
    override suspend fun detail(yatraId: String) = Result.failure<YatraDetailDto>(Exception("unused"))
}

private fun page(
    items: List<YatraSummaryDto>,
    offset: Int = 0,
    total: Int = items.size,
    hasMore: Boolean = false,
) = YatraCatalogPage(items, total, offset, hasMore)

private fun rows(count: Int, prefix: String = "page", published: Boolean = true) =
    List(count) { index -> yatra("$prefix-${index + 1}", published) }

private fun yatra(id: String, published: Boolean = true) = YatraSummaryDto(
    id = id,
    publicCode = "YT00000001",
    name = id,
    district = YatraDistrictDto("district-1", "Nashik", "NSK"),
    startsAt = "2026-07-01T00:00:00Z",
    endsAt = "2026-07-12T00:00:00Z",
    status = if (published) "upcoming" else "draft",
    published = published,
)

private class SignedInSession : SessionStore {
    override fun isLoggedIn(): Boolean = true
    override fun accessToken(): String = "token"
    override fun cachedProfile() = null
    override fun saveSession(token: String, profile: com.example.sahakarbharati.data.model.ParticipantProfile) = Unit
    override fun saveProfile(profile: com.example.sahakarbharati.data.model.ParticipantProfile) = Unit
    override fun clearSession() = Unit
}

private class CatalogEpochSession : SessionStore {
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
