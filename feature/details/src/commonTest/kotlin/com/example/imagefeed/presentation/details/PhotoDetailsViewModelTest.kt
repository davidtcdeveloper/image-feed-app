package com.example.imagefeed.presentation.details

import com.example.imagefeed.core.database.FavoritesRepository
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.details.model.HistoricalStats
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.details.model.StatMetric
import com.example.imagefeed.model.AppError
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.model.cache.NavigationPreviewCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-detail-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertNotNull(state.photo)
            assertEquals("p-detail-1", state.photo.id)
            assertNotNull(state.stats)
            assertEquals(42, state.stats.downloads.total)
            assertFalse(state.isFavorite)
            assertEquals(false, state.isLoading)
            assertNull(state.error)
            viewModel.clear()
        }

    @Test
    fun loadsInitialFavoriteStateWhenPhotoAlreadySaved() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-saved-1")
            val testStats = PhotoStats("p-saved-1", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository(initialPhotos = listOf(testPhoto))

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-saved-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()

            assertTrue(viewModel.state.value.isFavorite)
            viewModel.clear()
        }

    @Test
    fun observesExternalFavoriteChangesReactively() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-external-1")
            val testStats = PhotoStats("p-external-1", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel = PhotoDetailsViewModel("p-external-1", repository, downloadTracker, favoritesRepository)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isFavorite)

            favoritesRepository.toggleFavorite(testPhoto)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isFavorite)

            favoritesRepository.removeFavorite(testPhoto.id)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isFavorite)

            viewModel.clear()
        }

    @Test
    fun loadsLocalFavoriteFallbackWhenNetworkFails() =
        runTest(testDispatcher) {
            val savedPhoto = photo("p-offline-1")
            val repository = FakePhotoDetailsRepository(photoDetails = null, photoStats = null, shouldFail = true)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository(initialPhotos = listOf(savedPhoto))

            val viewModel = PhotoDetailsViewModel("p-offline-1", repository, downloadTracker, favoritesRepository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(savedPhoto, state.photo)
            assertTrue(state.isFavorite)
            assertFalse(state.isLoading)
            assertEquals(AppError.Network, state.error)

            viewModel.clear()
        }

    @Test
    fun toggleFavoritePersistsAndEmitsSnackbarEffects() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-fav-toggle")
            val testStats = PhotoStats("p-fav-toggle", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-fav-toggle",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()
            assertFalse(viewModel.state.value.isFavorite)

            viewModel.dispatch(PhotoDetailsIntent.ToggleFavorite())
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isFavorite)
            assertTrue(favoritesRepository.isFavorite(testPhoto.id))
            val savedEffect = viewModel.effects.first()
            assertEquals(PhotoDetailsEffect.ShowFavoriteToggledSnackbar(isFavorite = true), savedEffect)

            viewModel.dispatch(PhotoDetailsIntent.ToggleFavorite())
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isFavorite)
            assertFalse(favoritesRepository.isFavorite(testPhoto.id))
            val removedEffect = viewModel.effects.first()
            assertEquals(PhotoDetailsEffect.ShowFavoriteToggledSnackbar(isFavorite = false), removedEffect)
            viewModel.clear()
        }

    @Test
    fun toggleFavoriteWithFallbackPhotoWorksBeforeNetworkCompletes() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-inflight-1")
            val testStats = PhotoStats("p-inflight-1", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats).apply { delayMs = 500L }
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel = PhotoDetailsViewModel("p-inflight-1", repository, downloadTracker, favoritesRepository)
            testScheduler.advanceTimeBy(100L)
            assertNull(viewModel.state.value.photo)

            viewModel.dispatch(PhotoDetailsIntent.ToggleFavorite(fallbackPhoto = testPhoto))
            testScheduler.advanceTimeBy(50L)

            assertTrue(viewModel.state.value.isFavorite)
            assertTrue(favoritesRepository.isFavorite(testPhoto.id))
            assertEquals(PhotoDetailsEffect.ShowFavoriteToggledSnackbar(isFavorite = true), viewModel.effects.first())
            advanceUntilIdle()
            viewModel.clear()
        }

    @Test
    fun toggleFavoriteRemovesAlreadySavedPhotoWhenPhotoIsNull() =
        runTest(testDispatcher) {
            val savedPhoto = photo("p-remove-null-1")
            val testStats = PhotoStats("p-remove-null-1", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(savedPhoto, testStats).apply { delayMs = 500L }
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository =
                FakeFavoritesRepository(initialPhotos = listOf(savedPhoto)).apply {
                    returnNullOnLookup = true
                }

            val viewModel = PhotoDetailsViewModel("p-remove-null-1", repository, downloadTracker, favoritesRepository)
            testScheduler.advanceTimeBy(100L)
            assertNull(viewModel.state.value.photo)
            assertTrue(viewModel.state.value.isFavorite)

            viewModel.dispatch(PhotoDetailsIntent.ToggleFavorite())
            testScheduler.advanceTimeBy(50L)

            assertFalse(viewModel.state.value.isFavorite)
            assertFalse(favoritesRepository.isFavorite(savedPhoto.id))
            assertEquals(PhotoDetailsEffect.ShowFavoriteToggledSnackbar(isFavorite = false), viewModel.effects.first())
            advanceUntilIdle()
            viewModel.clear()
        }

    @Test
    fun toggleFavoriteStorageErrorEmitsErrorSnackbar() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-storage-err")
            val testStats = PhotoStats("p-storage-err", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository().apply { shouldFailWrite = true }

            val viewModel = PhotoDetailsViewModel("p-storage-err", repository, downloadTracker, favoritesRepository)
            advanceUntilIdle()

            viewModel.dispatch(PhotoDetailsIntent.ToggleFavorite())
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isFavorite)
            assertEquals(PhotoDetailsEffect.ShowErrorSnackbar(AppError.Network), viewModel.effects.first())
            viewModel.clear()
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-detail-2",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()

            viewModel.dispatch(PhotoDetailsIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(downloadTracker.downloadTracked)
            assertEquals("download-location-p-detail-2", downloadTracker.trackedUrl)
            viewModel.clear()
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-error-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.photo)
            assertEquals(AppError.Network, state.error)

            val effect = viewModel.effects.first()
            assertTrue(effect is PhotoDetailsEffect.ShowErrorSnackbar)
            assertEquals(AppError.Network, effect.error)
            viewModel.clear()
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-retry-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()
            assertEquals(AppError.Network, viewModel.state.value.error)

            repository.shouldFail = false
            viewModel.dispatch(PhotoDetailsIntent.Retry)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNotNull(state.photo)
            assertEquals("p-retry-1", state.photo.id)
            assertNull(state.error)
            viewModel.clear()
        }

    @Test
    fun clearCancelsFavoriteObservation() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-fav-clear")
            val testStats = PhotoStats("p-fav-clear", StatMetric(1, HistoricalStats()), StatMetric(1, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel = PhotoDetailsViewModel("p-fav-clear", repository, downloadTracker, favoritesRepository)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isFavorite)

            viewModel.clear()
            favoritesRepository.toggleFavorite(testPhoto)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isFavorite)
        }

    @Test
    fun clearInvokesTeardownCleanly() {
        val testPhoto = photo("p-clear-1")
        val testStats = PhotoStats("p-clear-1", StatMetric(1, HistoricalStats()), StatMetric(1, HistoricalStats()))
        val repository = FakePhotoDetailsRepository(testPhoto, testStats)
        val downloadTracker = FakePhotoDownloadTracker()
        val favoritesRepository = FakeFavoritesRepository()

        val viewModel = PhotoDetailsViewModel("p-clear-1", repository, downloadTracker, favoritesRepository)
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel = PhotoDetailsViewModel("p-concurrent-1", repository, downloadTracker, favoritesRepository)

            testScheduler.advanceTimeBy(100L)
            assertTrue(viewModel.state.value.isLoading)

            viewModel.dispatch(PhotoDetailsIntent.Retry)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.error)
            assertEquals("p-concurrent-1", state.photo?.id)
            viewModel.clear()
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel = PhotoDetailsViewModel("p-clear-2", repository, downloadTracker, favoritesRepository)
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
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel = PhotoDetailsViewModel("p-empty-download", repository, downloadTracker, favoritesRepository)
            advanceUntilIdle()

            viewModel.dispatch(PhotoDetailsIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(downloadTracker.downloadTracked)
            assertEquals("", downloadTracker.trackedUrl)
            viewModel.clear()
        }

    @Test
    fun observeFavoriteErrorEmitsErrorSnackbar() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-obs-err")
            val testStats = PhotoStats("p-obs-err", StatMetric(1, HistoricalStats()), StatMetric(1, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(testPhoto, testStats)
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository().apply { shouldFailObserve = true }

            val viewModel = PhotoDetailsViewModel("p-obs-err", repository, downloadTracker, favoritesRepository)
            advanceUntilIdle()

            assertEquals(PhotoDetailsEffect.ShowErrorSnackbar(AppError.Network), viewModel.effects.first())
            viewModel.clear()
        }

    @Test
    fun seedsPhotoSynchronouslyOnFrameZeroFromPreviewCache() =
        runTest(testDispatcher) {
            val previewPhoto = photo("p-frame0-1").copy(description = "cached-preview")
            val fullPhoto = previewPhoto.copy(description = "full-details")
            val testStats = PhotoStats("p-frame0-1", StatMetric(7, HistoricalStats()), StatMetric(14, HistoricalStats()))
            val repository = FakePhotoDetailsRepository(fullPhoto, testStats).apply { delayMs = 500L }
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()
            val previewCache = NavigationPreviewCache().apply { seedPhoto(previewPhoto) }

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-frame0-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                    previewCache = previewCache,
                )

            // Synchronous Frame-0 assertion before advancing the coroutine scheduler
            assertEquals(previewPhoto, viewModel.state.value.photo)
            assertTrue(viewModel.state.value.isLoading)

            advanceUntilIdle()

            assertEquals(fullPhoto, viewModel.state.value.photo)
            assertEquals(testStats, viewModel.state.value.stats)
            assertFalse(viewModel.state.value.isLoading)
            viewModel.clear()
        }

    @Test
    fun emitsPhotoDetailsConcurrentlyBeforeDelayedStatsComplete() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-parallel-1")
            val testStats = PhotoStats("p-parallel-1", StatMetric(30, HistoricalStats()), StatMetric(90, HistoricalStats()))
            val repository =
                FakePhotoDetailsRepository(testPhoto, testStats).apply {
                    detailsDelayMs = 100L
                    statsDelayMs = 500L
                }
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-parallel-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            testScheduler.advanceTimeBy(150L)
            testScheduler.runCurrent()

            // Details are visible immediately without blocking on slower stats
            assertEquals(testPhoto, viewModel.state.value.photo)
            assertNull(viewModel.state.value.stats)
            assertTrue(viewModel.state.value.isLoading)

            // Total elapsed time 550ms (< 600ms sequential sum) proves concurrent execution
            testScheduler.advanceTimeBy(400L)
            testScheduler.runCurrent()

            assertEquals(testPhoto, viewModel.state.value.photo)
            assertEquals(testStats, viewModel.state.value.stats)
            assertFalse(viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.error)
            viewModel.clear()
        }

    @Test
    fun surfacesErrorCleanlyWhenStatsFailAfterDetailsSucceed() =
        runTest(testDispatcher) {
            val testPhoto = photo("p-stats-fail-1")
            val testStats = PhotoStats("p-stats-fail-1", StatMetric(1, HistoricalStats()), StatMetric(2, HistoricalStats()))
            val repository =
                FakePhotoDetailsRepository(testPhoto, testStats).apply {
                    detailsDelayMs = 50L
                    statsDelayMs = 150L
                    shouldFailStats = true
                }
            val downloadTracker = FakePhotoDownloadTracker()
            val favoritesRepository = FakeFavoritesRepository()

            val viewModel =
                PhotoDetailsViewModel(
                    photoId = "p-stats-fail-1",
                    repository = repository,
                    downloadTracker = downloadTracker,
                    favoritesRepository = favoritesRepository,
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(testPhoto, state.photo)
            assertNull(state.stats)
            assertFalse(state.isLoading)
            assertEquals(AppError.Network, state.error)
            assertEquals(PhotoDetailsEffect.ShowErrorSnackbar(AppError.Network), viewModel.effects.first())
            viewModel.clear()
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
        var detailsDelayMs: Long? = null
        var statsDelayMs: Long? = null
        var shouldFailDetails: Boolean = false
        var shouldFailStats: Boolean = false

        override suspend fun getPhotoDetails(id: String): Photo {
            val effectiveDelay = detailsDelayMs ?: delayMs
            if (effectiveDelay > 0) kotlinx.coroutines.delay(effectiveDelay)
            if (shouldFail || shouldFailDetails) throw RuntimeException("Network error occurred")
            return photoDetails ?: error("No photo details")
        }

        override suspend fun getPhotoStats(id: String): PhotoStats {
            val effectiveDelay = statsDelayMs ?: delayMs
            if (effectiveDelay > 0) kotlinx.coroutines.delay(effectiveDelay)
            if (shouldFail || shouldFailStats) throw RuntimeException("Network error occurred")
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

    private class FakeFavoritesRepository(
        initialPhotos: List<Photo> = emptyList(),
    ) : FavoritesRepository {
        private val savedPhotos = MutableStateFlow(initialPhotos)
        var shouldFailWrite = false
        var shouldFailObserve = false
        var returnNullOnLookup = false

        override fun observeIsFavorite(photoId: String): Flow<Boolean> =
            if (shouldFailObserve) {
                flow { throw IllegalStateException("Storage observe error") }
            } else {
                savedPhotos.map { list -> list.any { photo -> photo.id == photoId } }
            }

        override fun observeFavoritePhotos(): Flow<List<Photo>> = savedPhotos

        override suspend fun getFavoriteById(photoId: String): Photo? =
            if (returnNullOnLookup) {
                null
            } else {
                savedPhotos.value.firstOrNull { photo -> photo.id == photoId }
            }

        override suspend fun isFavorite(photoId: String): Boolean = savedPhotos.value.any { photo -> photo.id == photoId }

        override suspend fun toggleFavorite(photo: Photo): Boolean {
            if (shouldFailWrite) throw IllegalStateException("Storage write error")
            val exists = isFavorite(photo.id)
            if (exists) {
                savedPhotos.update { list -> list.filterNot { item -> item.id == photo.id } }
            } else {
                savedPhotos.update { list -> listOf(photo) + list.filterNot { item -> item.id == photo.id } }
            }
            return !exists
        }

        override suspend fun removeFavorite(photoId: String) {
            if (shouldFailWrite) throw IllegalStateException("Storage write error")
            savedPhotos.update { list -> list.filterNot { item -> item.id == photoId } }
        }
    }
}
