package com.example.imagefeed.presentation.details

import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.details.model.HistoricalStats
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.details.model.StatMetric
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
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
                FakePhotoDetailsRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                )
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-detail-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
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
    fun trackDownloadTriggersTrackerCall() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-detail-2")
            val testStats =
                PhotoStats(
                    id = "p-detail-2",
                    downloads = StatMetric(1, HistoricalStats()),
                    views = StatMetric(1, HistoricalStats()),
                )
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-detail-2",
                    repository = repository,
                    downloadTracker = downloadTracker,
                )

            advanceUntilIdle()

            viewModel.dispatch(PhotoDetailsIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(downloadTracker.downloadTracked)
            assertEquals("download-location-p-detail-2", downloadTracker.trackedUrl)
        }

    @Test
    fun errorStateEmitsEffectAndUpdatesState() =
        runTest(testDispatcher) {
            val repository =
                FakePhotoDetailsRepository(
                    photoDetails = null,
                    photoStats = null,
                    shouldFail = true,
                )
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-error-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.photo)
            assertEquals(com.example.imagefeed.model.AppError.Network, state.error)

            val effect = viewModel.effects.first()
            assertTrue(effect is PhotoDetailsEffect.ShowErrorSnackbar)
            assertEquals(com.example.imagefeed.model.AppError.Network, effect.error)
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
                FakePhotoDetailsRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                    shouldFail = true,
                )
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-retry-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                )

            advanceUntilIdle()
            assertEquals(com.example.imagefeed.model.AppError.Network, viewModel.state.value.error)

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
        val repository = FakePhotoDetailsRepository(testPhoto, testStats)
        val downloadTracker = FakePhotoDownloadTracker()

        val viewModel = PhotoDetailsViewModel("p-clear-1", repository, downloadTracker)
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
                FakePhotoDetailsRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                ).apply { delayMs = 500L }
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel = PhotoDetailsViewModel("p-concurrent-1", repository, downloadTracker)

            testScheduler.advanceTimeBy(100L)
            assertTrue(viewModel.state.value.isLoading)

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
                FakePhotoDetailsRepository(
                    photoDetails = testPhoto,
                    photoStats = testStats,
                ).apply { delayMs = 500L }
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel = PhotoDetailsViewModel("p-clear-2", repository, downloadTracker)
            testScheduler.advanceTimeBy(100L)
            assertTrue(viewModel.state.value.isLoading)

            viewModel.clear()
            advanceUntilIdle()

            assertNull(viewModel.state.value.photo)
            assertNull(viewModel.state.value.error)
        }

    @Test
    fun trackDownloadWithEmptyLocationDelegatesSafely() =
        runTest(testDispatcher) {
            val testPhoto =
                photo("p-empty-download").copy(
                    links = PhotoLinks("self", "html", "download", ""),
                )
            val testStats = PhotoStats("p-empty-download", StatMetric(1, HistoricalStats()), StatMetric(1, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()

            val viewModel = PhotoDetailsViewModel("p-empty-download", repository, downloadTracker)
            advanceUntilIdle()

            viewModel.dispatch(PhotoDetailsIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(downloadTracker.downloadTracked)
            assertEquals("", downloadTracker.trackedUrl)
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

    private class FakePhotoDetailsRepository(
        private val photoDetails: Photo?,
        private val photoStats: PhotoStats?,
        var shouldFail: Boolean = false,
    ) : PhotoDetailsRepository {
        var delayMs: Long = 0L

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
    }

    private class FakePhotoDownloadTracker : PhotoDownloadTracker {
        var downloadTracked = false
        var trackedUrl: String? = null

        override suspend fun trackDownload(downloadLocationUrl: String) {
            downloadTracked = true
            trackedUrl = downloadLocationUrl
        }
    }
}
