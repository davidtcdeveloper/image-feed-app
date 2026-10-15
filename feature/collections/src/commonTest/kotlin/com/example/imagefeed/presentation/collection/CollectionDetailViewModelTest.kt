package com.example.imagefeed.presentation.collection

import com.example.imagefeed.collections.data.CollectionsRepository
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.model.CollectionLinks
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailViewModelTest {
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
    fun loadsCollectionHeaderPhotosAndRelatedSuccessfully() =
        runTest(testDispatcher) {
            val testCollection =
                PhotoCollection(
                    id = "col-1",
                    title = "Test Collection",
                    totalPhotos = 10,
                    user = user(),
                    links = CollectionLinks("self", "html", "photos"),
                )
            val testPhotos = listOf(photo("p-col-1"), photo("p-col-2"))
            val testRelated =
                listOf(
                    PhotoCollection(
                        id = "col-related-1",
                        title = "Related Collection",
                        totalPhotos = 3,
                        user = user(),
                        links = CollectionLinks("self", "html", "photos"),
                    ),
                )

            val repository =
                FakeCollectionsRepository(
                    collection = testCollection,
                    photos = testPhotos,
                    related = testRelated,
                )
            val tracker = FakePhotoDownloadTracker()

            val viewModel = CollectionDetailViewModel("col-1", repository, tracker)
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.collection)
            assertEquals(
                "Test Collection",
                viewModel.state.value.collection
                    ?.title,
            )
            assertEquals(2, viewModel.state.value.photos.size)
            assertEquals(1, viewModel.state.value.related.size)
            assertEquals(
                "Related Collection",
                viewModel.state.value.related[0]
                    .title,
            )
        }

    @Test
    fun handlesErrorGracefully() =
        runTest(testDispatcher) {
            val repository = FakeCollectionsRepository(shouldFail = true)
            val tracker = FakePhotoDownloadTracker()
            val viewModel = CollectionDetailViewModel("col-fail", repository, tracker)
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.error)
            assertTrue(
                viewModel.state.value.error
                    ?.endsWith("failure") == true,
            )
        }

    @Test
    fun trackDownloadInvokesDownloadTracker() =
        runTest(testDispatcher) {
            val repository = FakeCollectionsRepository()
            val tracker = FakePhotoDownloadTracker()
            val viewModel = CollectionDetailViewModel("col-1", repository, tracker)
            advanceUntilIdle()

            val testPhoto = photo("p-download")
            viewModel.dispatch(CollectionDetailIntent.TrackDownload(testPhoto))
            advanceUntilIdle()

            assertTrue(tracker.downloadTracked)
            assertEquals("download-location-p-download", tracker.trackedUrl)
        }

    @Test
    fun refreshReloadsCollection() =
        runTest(testDispatcher) {
            val testCollection =
                PhotoCollection(
                    id = "col-1",
                    title = "Test Collection",
                    totalPhotos = 10,
                    user = user(),
                    links = CollectionLinks("self", "html", "photos"),
                )
            val repository =
                FakeCollectionsRepository(
                    collection = testCollection,
                    photos = listOf(photo("p-1")),
                )
            val tracker = FakePhotoDownloadTracker()
            val viewModel = CollectionDetailViewModel("col-1", repository, tracker)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.photos.size)

            viewModel.dispatch(CollectionDetailIntent.Refresh)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.photos.size)
        }

    private class FakeCollectionsRepository(
        private val collection: PhotoCollection? = null,
        private val photos: List<Photo> = emptyList(),
        private val related: List<PhotoCollection> = emptyList(),
        private val shouldFail: Boolean = false,
    ) : CollectionsRepository {
        override suspend fun getCollections(
            page: Int,
            perPage: Int,
        ): List<PhotoCollection> = emptyList()

        override suspend fun getCollection(id: String): PhotoCollection {
            if (shouldFail) throw RuntimeException("Collection failure")
            return collection ?: error("No collection")
        }

        override suspend fun getCollectionPhotos(
            id: String,
            page: Int,
            perPage: Int,
        ): List<Photo> {
            if (shouldFail) throw RuntimeException("Photos failure")
            return photos
        }

        override suspend fun getRelatedCollections(id: String): List<PhotoCollection> {
            if (shouldFail) throw RuntimeException("Related failure")
            return related
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
