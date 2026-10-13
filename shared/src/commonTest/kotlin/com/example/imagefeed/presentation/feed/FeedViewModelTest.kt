package com.example.imagefeed.presentation.feed

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.Topic
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.model.UserStats
import com.example.imagefeed.repository.UnsplashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun user() =
        User(
            id = "user-1",
            username = "user-1",
            name = "User One",
            profileImage = ProfileImage("small", "medium", "large"),
            links = UserLinks("self", "html", "photos"),
        )

    private fun photo(id: String) =
        Photo(
            id = id,
            width = 100,
            height = 100,
            urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
            user = user(),
            links = PhotoLinks("self", "html", "download", "download-location-$id"),
        )

    @Test
    fun loadsInitialFeedAndTopics() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    topics = listOf(Topic("t1", "nature", "Nature", totalPhotos = 2)),
                    photosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("p1"), photo("p2")))),
                )

            val viewModel = FeedViewModel(repository)
            advanceUntilIdle()

            assertEquals(
                listOf("Nature"),
                viewModel.state.value.topics
                    .map { it.title },
            )
            assertEquals(2, viewModel.state.value.photos.size)
            assertEquals("editorial", viewModel.state.value.selectedTopicSlug)
            assertTrue(
                viewModel.state.value.photos
                    .any { it.id == "p1" },
            )
        }

    @Test
    fun selectingTopicLoadsTopicSpecificPhotos() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    topics = listOf(Topic("t1", "nature", "Nature", totalPhotos = 2)),
                    photosByPage =
                        mapOf(
                            "editorial" to mapOf(1 to listOf(photo("editorial-1"))),
                            "nature" to mapOf(1 to listOf(photo("nature-1"))),
                        ),
                )

            val viewModel = FeedViewModel(repository)
            advanceUntilIdle()

            viewModel.dispatch(FeedIntent.SelectTopic("nature"))
            advanceUntilIdle()

            assertEquals("nature", viewModel.state.value.selectedTopicSlug)
            assertEquals(
                listOf("nature-1"),
                viewModel.state.value.photos
                    .map { it.id },
            )
        }

    @Test
    fun refreshReplacesFeedContent() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    topics = emptyList(),
                    photosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("old-1")))),
                    refreshedPhotosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("fresh-1"), photo("fresh-2")))),
                )

            val viewModel = FeedViewModel(repository)
            advanceUntilIdle()

            viewModel.dispatch(FeedIntent.Refresh)
            advanceUntilIdle()

            assertEquals(
                listOf("fresh-1", "fresh-2"),
                viewModel.state.value.photos
                    .map { it.id },
            )
            assertEquals(1, viewModel.state.value.page)
        }

    @Test
    fun trackDownloadInvokesRepository() =
        runTest(testDispatcher) {
            val repository = FakeUnsplashRepository()
            val viewModel = FeedViewModel(repository)
            advanceUntilIdle()

            val testPhoto = photo("p-track")
            viewModel.dispatch(FeedIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(repository.downloadTracked)
        }

    private class FakeUnsplashRepository(
        private val topics: List<Topic> = emptyList(),
        private val photosByPage: Map<String, Map<Int, List<Photo>>> = emptyMap(),
        private val refreshedPhotosByPage: Map<String, Map<Int, List<Photo>>> = emptyMap(),
    ) : UnsplashRepository {
        var downloadTracked = false
        private var isRefreshed = false

        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> {
            val active = if (isRefreshed && refreshedPhotosByPage.containsKey("editorial")) refreshedPhotosByPage else photosByPage
            isRefreshed = true
            return active["editorial"]?.get(page) ?: emptyList()
        }

        override suspend fun getPhotoDetails(id: String): Photo = error("Not used")

        override suspend fun getPhotoStats(id: String): PhotoStats = error("Not used")

        override suspend fun getRandomPhotos(
            orientation: String?,
            query: String?,
            count: Int?,
        ): List<Photo> = emptyList()

        override suspend fun trackDownload(downloadLocationUrl: String) {
            downloadTracked = true
        }

        override suspend fun getCollections(
            page: Int,
            perPage: Int,
        ): List<PhotoCollection> = emptyList()

        override suspend fun getCollection(id: String): PhotoCollection = error("Not used")

        override suspend fun getCollectionPhotos(
            id: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getRelatedCollections(id: String): List<PhotoCollection> = emptyList()

        override suspend fun getTopics(
            page: Int,
            perPage: Int,
        ): List<Topic> = topics

        override suspend fun getTopic(idOrSlug: String): Topic = error("Not used")

        override suspend fun getTopicPhotos(
            idOrSlug: String,
            page: Int,
            perPage: Int,
        ): List<Photo> {
            val active = if (isRefreshed && refreshedPhotosByPage.containsKey(idOrSlug)) refreshedPhotosByPage else photosByPage
            isRefreshed = true
            return active[idOrSlug]?.get(page) ?: emptyList()
        }

        override suspend fun getUserProfile(username: String): User = error("Not used")

        override suspend fun getUserPhotos(
            username: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getUserLikes(
            username: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getUserCollections(
            username: String,
            page: Int,
            perPage: Int,
        ): List<PhotoCollection> = emptyList()

        override suspend fun getUserStats(username: String): UserStats = error("Not used")

        override suspend fun searchPhotos(
            query: String,
            page: Int,
            perPage: Int,
            orderBy: String?,
            color: String?,
            orientation: String?,
            contentFilter: String?,
        ): SearchResponse<Photo> = error("Not used")

        override suspend fun searchCollections(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<com.example.imagefeed.model.CollectionSummary> = error("Not used")

        override suspend fun searchUsers(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<User> = error("Not used")
    }
}
