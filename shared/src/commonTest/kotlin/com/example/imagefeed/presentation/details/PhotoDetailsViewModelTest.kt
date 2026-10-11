package com.example.imagefeed.presentation.details

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDetailsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPhotoDetailsAndStatsSuccessfully() =
        runTest(testDispatcher) {
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

            val viewModel =
                PhotoDetailsViewModel(
                    repository = repository,
                    photoId = "p-detail-1",
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertNotNull(state.photo)
            assertEquals("p-detail-1", state.photo.id)
            assertNotNull(state.stats)
            assertEquals(42, state.stats.downloads.total)
            assertEquals(false, state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun trackDownloadTriggersRepositoryCall() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-detail-2")
            val testStats =
                PhotoStats(
                    id = "p-detail-2",
                    downloads = StatMetric(1, HistoricalStats()),
                    views = StatMetric(1, HistoricalStats()),
                )
            val repository = FakeUnsplashRepository(testPhoto, testStats)

            val viewModel =
                PhotoDetailsViewModel(
                    repository = repository,
                    photoId = "p-detail-2",
                )

            advanceUntilIdle()

            viewModel.dispatch(PhotoDetailsIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(repository.downloadTracked)
        }

    @Test
    fun errorStateEmitsEffectAndUpdatesState() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    photoDetails = null,
                    photoStats = null,
                    shouldFail = true,
                )

            val viewModel =
                PhotoDetailsViewModel(
                    repository = repository,
                    photoId = "p-error-1",
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.photo)
            assertEquals("Network error occurred", state.error)

            val effect = viewModel.effects.first()
            assertTrue(effect is PhotoDetailsEffect.ShowErrorSnackbar)
            assertEquals("Network error occurred", effect.message)
        }

    @Test
    fun retryLoadsDetailsAfterFailure() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-retry-1")
            val testStats =
                PhotoStats(
                    id = "p-retry-1",
                    downloads = StatMetric(10, HistoricalStats()),
                    views = StatMetric(20, HistoricalStats()),
                )

            val repository =
                FakeUnsplashRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                    shouldFail = true,
                )

            val viewModel =
                PhotoDetailsViewModel(
                    repository = repository,
                    photoId = "p-retry-1",
                )

            advanceUntilIdle()
            assertEquals("Network error occurred", viewModel.state.value.error)

            repository.shouldFail = false
            viewModel.dispatch(PhotoDetailsIntent.Retry)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNotNull(state.photo)
            assertEquals("p-retry-1", state.photo.id)
            assertNull(state.error)
        }

    @Test
    fun clearInvokesTeardownCleanly() {
        val testPhoto = photo("p-clear-1")
        val testStats = PhotoStats("p-clear-1", StatMetric(1, HistoricalStats()), StatMetric(1, HistoricalStats()))
        val repository = FakeUnsplashRepository(testPhoto, testStats)

        val viewModel = PhotoDetailsViewModel(repository, "p-clear-1")
        viewModel.clear()
    }

    @Test
    fun retryCancelsInFlightRequestWithoutSpuriousCancellationError() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-concurrent-1")
            val testStats =
                PhotoStats(
                    id = "p-concurrent-1",
                    downloads = StatMetric(5, HistoricalStats()),
                    views = StatMetric(15, HistoricalStats()),
                )
            val repository =
                FakeUnsplashRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                ).apply { delayMs = 500L }

            val viewModel = PhotoDetailsViewModel(repository, "p-concurrent-1")

            // Advance partially so the first request is suspending
            testScheduler.advanceTimeBy(100L)
            assertTrue(viewModel.state.value.isLoading)

            // Dispatch Retry while in flight: should cancel first without throwing CancellationException as UI error
            viewModel.dispatch(PhotoDetailsIntent.Retry)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.error)
            assertEquals("p-concurrent-1", state.photo?.id)
        }

    @Test
    fun clearCancelsInFlightRequestDeterministically() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-clear-2")
            val testStats =
                PhotoStats(
                    id = "p-clear-2",
                    downloads = StatMetric(2, HistoricalStats()),
                    views = StatMetric(4, HistoricalStats()),
                )
            val repository =
                FakeUnsplashRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                ).apply { delayMs = 500L }

            val viewModel = PhotoDetailsViewModel(repository, "p-clear-2")
            testScheduler.advanceTimeBy(100L)
            assertTrue(viewModel.state.value.isLoading)

            viewModel.clear()
            advanceUntilIdle()

            // State should not have completed with success or error
            assertNull(viewModel.state.value.photo)
            assertNull(viewModel.state.value.error)
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
        private val photoDetails: Photo?,
        private val photoStats: PhotoStats?,
        var shouldFail: Boolean = false,
    ) : UnsplashRepository {
        var downloadTracked = false
        var delayMs: Long = 0L

        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getPhotoDetails(id: String): Photo {
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            if (shouldFail) throw RuntimeException("Network error occurred")
            return photoDetails ?: error("No photo details")
        }

        override suspend fun getPhotoStats(id: String): PhotoStats {
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            if (shouldFail) throw RuntimeException("Network error occurred")
            return photoStats ?: error("No photo stats")
        }

        override suspend fun getRandomPhotos(
            orientation: String?,
            query: String?,
            count: Int?,
        ): List<Photo> = emptyList()

        override suspend fun trackDownload(downloadLocationUrl: String) {
            if (downloadLocationUrl == photoDetails?.links?.downloadLocation) {
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
