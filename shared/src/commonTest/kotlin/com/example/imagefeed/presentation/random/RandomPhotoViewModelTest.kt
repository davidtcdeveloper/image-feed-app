package com.example.imagefeed.presentation.random

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
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
            val repository = FakeUnsplashRepository(randomPhotos = listOf(photo))
            val viewModel = RandomPhotoViewModel(repository)
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
            val repository = FakeUnsplashRepository(shouldFail = true)
            val viewModel = RandomPhotoViewModel(repository)
            advanceUntilIdle()

            assertEquals("Random failure", viewModel.state.value.error)
        }

    @Test
    fun trackDownloadInvokesRepository() =
        runTest(testDispatcher) {
            val photo = testPhoto("random-1")
            val repository = FakeUnsplashRepository(randomPhotos = listOf(photo))
            val viewModel = RandomPhotoViewModel(repository)
            advanceUntilIdle()

            viewModel.dispatch(RandomPhotoIntent.TrackDownload)
            advanceUntilIdle()

            assertTrue(repository.downloadTracked)
        }

    private class FakeUnsplashRepository(
        private val randomPhotos: List<Photo> = emptyList(),
        private val shouldFail: Boolean = false,
    ) : UnsplashRepository {
        var downloadTracked = false

        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getPhotoDetails(id: String): Photo = error("Not used")

        override suspend fun getPhotoStats(id: String): com.example.imagefeed.model.PhotoStats = error("Not used")

        override suspend fun getRandomPhotos(
            orientation: String?,
            query: String?,
            count: Int?,
        ): List<Photo> {
            if (shouldFail) throw RuntimeException("Random failure")
            return randomPhotos
        }

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
