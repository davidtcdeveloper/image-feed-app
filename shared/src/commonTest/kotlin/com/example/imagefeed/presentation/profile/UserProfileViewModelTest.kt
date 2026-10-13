package com.example.imagefeed.presentation.profile

import com.example.imagefeed.model.HistoricalStats
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
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

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {
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
            id = "user-alice",
            username = "alice",
            name = "Alice Test",
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
    fun loadsUserProfileAndDefaultPortfolioPhotos() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    user = testUser(),
                    userPhotos = listOf(testPhoto("p-1"), testPhoto("p-2")),
                )
            val viewModel = UserProfileViewModel(repository, "alice")
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.user)
            assertEquals(
                "alice",
                viewModel.state.value.user
                    ?.username,
            )
            assertEquals(2, viewModel.state.value.portfolioPhotos.size)
            assertEquals(ProfileTab.PORTFOLIO, viewModel.state.value.activeTab)
        }

    @Test
    fun switchesToLikesTabAndLoadsLikes() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    user = testUser(),
                    userLikes = listOf(testPhoto("p-liked")),
                )
            val viewModel = UserProfileViewModel(repository, "alice")
            advanceUntilIdle()

            viewModel.dispatch(UserProfileIntent.SelectTab(ProfileTab.LIKES))
            advanceUntilIdle()

            assertEquals(ProfileTab.LIKES, viewModel.state.value.activeTab)
            assertEquals(1, viewModel.state.value.likedPhotos.size)
        }

    @Test
    fun switchesToInsightsTabAndLoadsStats() =
        runTest(testDispatcher) {
            val stats =
                UserStats(
                    username = "alice",
                    downloads = StatMetric(500, HistoricalStats()),
                    views = StatMetric(1000, HistoricalStats()),
                )
            val repository =
                FakeUnsplashRepository(
                    user = testUser(),
                    userStats = stats,
                )
            val viewModel = UserProfileViewModel(repository, "alice")
            advanceUntilIdle()

            viewModel.dispatch(UserProfileIntent.SelectTab(ProfileTab.INSIGHTS))
            advanceUntilIdle()

            assertEquals(ProfileTab.INSIGHTS, viewModel.state.value.activeTab)
            assertNotNull(viewModel.state.value.stats)
            assertEquals(
                500,
                viewModel.state.value.stats
                    ?.downloads
                    ?.total,
            )
        }

    @Test
    fun handlesErrorGracefully() =
        runTest(testDispatcher) {
            val repository = FakeUnsplashRepository(shouldFail = true)
            val viewModel = UserProfileViewModel(repository, "alice")
            advanceUntilIdle()

            assertEquals("User error", viewModel.state.value.error)
        }

    private class FakeUnsplashRepository(
        private val user: User? = null,
        private val userPhotos: List<Photo> = emptyList(),
        private val userLikes: List<Photo> = emptyList(),
        private val userCollections: List<PhotoCollection> = emptyList(),
        private val userStats: UserStats? = null,
        private val shouldFail: Boolean = false,
    ) : UnsplashRepository {
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
        ): List<Photo> = emptyList()

        override suspend fun trackDownload(downloadLocationUrl: String) = Unit

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

        override suspend fun getUserProfile(username: String): User {
            if (shouldFail) throw RuntimeException("User error")
            return user ?: error("No user")
        }

        override suspend fun getUserPhotos(
            username: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = userPhotos

        override suspend fun getUserLikes(
            username: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = userLikes

        override suspend fun getUserCollections(
            username: String,
            page: Int,
            perPage: Int,
        ): List<PhotoCollection> = userCollections

        override suspend fun getUserStats(username: String): UserStats = userStats ?: error("No stats")

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
