package com.example.imagefeed.presentation.random

import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.feed.data.FeedRepository
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.Topic
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
class RandomPhotoViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testUser() =
        User(
            id = "user-1",
            username = "user-1",
            name = "User One",
            profileImage = ProfileImage("small", "medium", "large"),
            links = UserLinks("self", "html", "photos"),
        )

    private fun testPhoto(id: String) =
        Photo(
            id = id,
            width = 100,
            height = 100,
            urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
            user = testUser(),
            links = PhotoLinks("self", "html", "download", "download-location-$id"),
        )

    @Test
    fun fetchesRandomPhotoSuccessfully() =
        runTest(testDispatcher) {
            val photo = testPhoto("random-1")
            val repository = FakeFeedRepository(randomPhotos = listOf(photo))
            val tracker = FakePhotoDownloadTracker()
            val viewModel = RandomPhotoViewModel(repository, tracker)
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.photo)
            assertEquals(
                "random-1",
                viewModel.state.value.photo
                    ?.id,
            )
        }

    @Test
    fun handlesFetchFailureGracefully() =
        runTest(testDispatcher) {
            val repository = FakeFeedRepository(shouldFail = true)
            val tracker = FakePhotoDownloadTracker()
            val viewModel = RandomPhotoViewModel(repository, tracker)
            advanceUntilIdle()

            assertEquals("Random failure", viewModel.state.value.error)
        }

    @Test
    fun trackDownloadInvokesDownloadTracker() =
        runTest(testDispatcher) {
            val photo = testPhoto("random-1")
            val repository = FakeFeedRepository(randomPhotos = listOf(photo))
            val tracker = FakePhotoDownloadTracker()
            val viewModel = RandomPhotoViewModel(repository, tracker)
            advanceUntilIdle()

            viewModel.dispatch(RandomPhotoIntent.TrackDownload)
            advanceUntilIdle()

            assertTrue(tracker.downloadTracked)
            assertEquals("download-location-random-1", tracker.trackedUrl)
        }

    private class FakeFeedRepository(
        private val randomPhotos: List<Photo> = emptyList(),
        private val shouldFail: Boolean = false,
    ) : FeedRepository {
        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getRandomPhotos(
            orientation: String?,
            query: String?,
            count: Int?,
        ): List<Photo> {
            if (shouldFail) throw RuntimeException("Random failure")
            return randomPhotos
        }

        override suspend fun getTopics(
            page: Int,
            perPage: Int,
        ): List<Topic> = emptyList()

        override suspend fun getTopicPhotos(
            idOrSlug: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()
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
