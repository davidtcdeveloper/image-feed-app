package com.example.imagefeed.presentation

import com.example.imagefeed.model.HistoricalStats
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.StatMetric
import com.example.imagefeed.model.Topic
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.model.UserStats
import com.example.imagefeed.repository.UnsplashRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDetailsPresenterTest {
    @Test
    fun loadsPhotoDetailsAndStatsSuccessfully() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val testPhoto = photo("p-detail-1")
            val testStats =
                PhotoStats(
                    id = "p-detail-1",
                    downloads = StatMetric(42, HistoricalStats()),
                    views = StatMetric(100, HistoricalStats()),
                )

            val repository =
                FakeUnsplashRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                )

            val presenter =
                PhotoDetailsPresenter(
                    repository = repository,
                    photoId = "p-detail-1",
                    presenterScopeFactory = TestPresenterScopeFactory(dispatcher),
                )

            advanceUntilIdle()

            assertNotNull(presenter.state.value.photo)
            assertEquals(
                "p-detail-1",
                presenter.state.value.photo
                    ?.id,
            )
            assertNotNull(presenter.state.value.stats)
            assertEquals(
                42,
                presenter.state.value.stats
                    ?.downloads
                    ?.total,
            )
        }

    @Test
    fun trackDownloadTriggersRepositoryCall() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val testPhoto = photo("p-detail-2")
            val testStats = PhotoStats("p-detail-2", StatMetric(1, HistoricalStats()), StatMetric(1, HistoricalStats()))
            val repository = FakeUnsplashRepository(testPhoto, testStats)

            val presenter =
                PhotoDetailsPresenter(
                    repository = repository,
                    photoId = "p-detail-2",
                    presenterScopeFactory = TestPresenterScopeFactory(dispatcher),
                )

            advanceUntilIdle()
            presenter.trackDownload()
            advanceUntilIdle()

            assertTrue(repository.downloadTracked)
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
            links = PhotoLinks("self", "html", "download", "download-location-$id"),
        )

    private class FakeUnsplashRepository(
        private val photoDetails: Photo,
        private val photoStats: PhotoStats,
    ) : UnsplashRepository {
        var downloadTracked = false

        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getPhotoDetails(id: String): Photo = photoDetails

        override suspend fun getPhotoStats(id: String): PhotoStats = photoStats

        override suspend fun getRandomPhotos(
            orientation: String?,
            query: String?,
            count: Int?,
        ): List<Photo> = emptyList()

        override suspend fun trackDownload(downloadLocationUrl: String) {
            if (downloadLocationUrl == photoDetails.links.downloadLocation) {
                downloadTracked = true
            }
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
        ): List<Topic> = emptyList()

        override suspend fun getTopic(idOrSlug: String): Topic = error("Not used")

        override suspend fun getTopicPhotos(
            idOrSlug: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

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
