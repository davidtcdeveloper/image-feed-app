package com.example.imagefeed.presentation

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedPresenterTest {
    @Test
    fun loadsInitialFeedAndTopics() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository =
                FakeUnsplashRepository(
                    topics = listOf(Topic("t1", "nature", "Nature", totalPhotos = 2)),
                    photosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("p1"), photo("p2")))),
                )

            val presenter = FeedPresenter(repository, CoroutineScope(dispatcher + SupervisorJob()))

            advanceUntilIdle()

            assertEquals(
                listOf("Nature"),
                presenter.state.value.topics
                    .map { it.title },
            )
            assertEquals(2, presenter.state.value.photos.size)
            assertEquals("editorial", presenter.state.value.selectedTopicSlug)
            assertTrue(
                presenter.state.value.photos
                    .map { it.id }
                    .contains("p1"),
            )
        }

    @Test
    fun refreshReplacesFeedContent() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository =
                FakeUnsplashRepository(
                    topics = emptyList(),
                    photosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("old-1")))),
                    refreshedPhotosByPage = mapOf("editorial" to mapOf(1 to listOf(photo("fresh-1"), photo("fresh-2")))),
                )

            val presenter = FeedPresenter(repository, CoroutineScope(dispatcher + SupervisorJob()))
            advanceUntilIdle()

            presenter.refresh()
            advanceUntilIdle()

            assertEquals(
                listOf("fresh-1", "fresh-2"),
                presenter.state.value.photos
                    .map { it.id },
            )
            assertEquals(1, presenter.state.value.page)
        }

    @Test
    fun selectingTopicLoadsTopicSpecificPhotos() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository =
                FakeUnsplashRepository(
                    topics = listOf(Topic("t1", "nature", "Nature", totalPhotos = 2)),
                    photosByPage =
                        mapOf(
                            "editorial" to mapOf(1 to listOf(photo("editorial-1"))),
                            "nature" to mapOf(1 to listOf(photo("nature-1"))),
                        ),
                )

            val presenter = FeedPresenter(repository, CoroutineScope(dispatcher + SupervisorJob()))
            advanceUntilIdle()

            presenter.selectTopic("nature")
            advanceUntilIdle()

            assertEquals("nature", presenter.state.value.selectedTopicSlug)
            assertEquals(
                listOf("nature-1"),
                presenter.state.value.photos
                    .map { it.id },
            )
            assertEquals(1, presenter.state.value.page)
        }

    private fun photo(id: String) =
        Photo(
            id = id,
            width = 100,
            height = 100,
            urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
            user =
                User(
                    id = "user-$id",
                    username = "user-$id",
                    name = "User $id",
                    profileImage = ProfileImage("small", "medium", "large"),
                    links = UserLinks("self", "html", "photos"),
                ),
            links = PhotoLinks("self", "html", "download", "download-location"),
        )

    private class FakeUnsplashRepository(
        private val topics: List<Topic>,
        private val photosByPage: Map<String, Map<Int, List<Photo>>>,
        private val refreshedPhotosByPage: Map<String, Map<Int, List<Photo>>> = photosByPage,
    ) : UnsplashRepository {
        private var photosCallCount = 0

        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> {
            photosCallCount += 1
            val source = if (photosCallCount == 1) photosByPage else refreshedPhotosByPage
            return source["editorial"]?.get(page) ?: emptyList()
        }

        override suspend fun getPhotoDetails(id: String): Photo = error("Not used in these tests")

        override suspend fun getPhotoStats(id: String): PhotoStats = error("Not used in these tests")

        override suspend fun getRandomPhotos(
            orientation: String?,
            query: String?,
            count: Int?,
        ): List<Photo> = emptyList()

        override suspend fun trackDownload(downloadLocationUrl: String) = Unit

        override suspend fun getCollections(
            page: Int,
            perPage: Int,
        ): List<PhotoCollection> = emptyList()

        override suspend fun getCollection(id: String): PhotoCollection = error("Not used in these tests")

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

        override suspend fun getTopic(idOrSlug: String): Topic = error("Not used in these tests")

        override suspend fun getTopicPhotos(
            idOrSlug: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = photosByPage[idOrSlug]?.get(page) ?: emptyList()

        override suspend fun getUserProfile(username: String): User = error("Not used in these tests")

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

        override suspend fun getUserStats(username: String): UserStats = error("Not used in these tests")

        override suspend fun searchPhotos(
            query: String,
            page: Int,
            perPage: Int,
            orderBy: String?,
            color: String?,
            orientation: String?,
            contentFilter: String?,
        ): SearchResponse<Photo> = error("Not used in these tests")

        override suspend fun searchCollections(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<com.example.imagefeed.model.CollectionSummary> = error("Not used in these tests")

        override suspend fun searchUsers(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<User> = error("Not used in these tests")
    }
}
