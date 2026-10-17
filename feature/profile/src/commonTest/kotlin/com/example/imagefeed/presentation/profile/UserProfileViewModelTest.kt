package com.example.imagefeed.presentation.profile

import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.model.HistoricalStats
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.StatMetric
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.profile.data.ProfileRepository
import com.example.imagefeed.profile.model.UserStats
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
                FakeProfileRepository(
                    user = testUser(),
                    userPhotos = listOf(testPhoto("p-1"), testPhoto("p-2")),
                )
            val tracker = FakePhotoDownloadTracker()
            val viewModel = UserProfileViewModel("alice", repository, tracker)
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
                FakeProfileRepository(
                    user = testUser(),
                    userLikes = listOf(testPhoto("p-liked")),
                )
            val tracker = FakePhotoDownloadTracker()
            val viewModel = UserProfileViewModel("alice", repository, tracker)
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
                FakeProfileRepository(
                    user = testUser(),
                    userStats = stats,
                )
            val tracker = FakePhotoDownloadTracker()
            val viewModel = UserProfileViewModel("alice", repository, tracker)
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
            val repository = FakeProfileRepository(shouldFail = true)
            val tracker = FakePhotoDownloadTracker()
            val viewModel = UserProfileViewModel("alice", repository, tracker)
            advanceUntilIdle()

            assertEquals(com.example.imagefeed.model.AppError.Network, viewModel.state.value.error)
        }

    @Test
    fun trackDownloadInvokesDownloadTracker() =
        runTest(testDispatcher) {
            val repository = FakeProfileRepository()
            val tracker = FakePhotoDownloadTracker()
            val viewModel = UserProfileViewModel("alice", repository, tracker)
            advanceUntilIdle()

            val photo = testPhoto("p-prof-dl")
            viewModel.dispatch(UserProfileIntent.TrackDownload(photo))
            advanceUntilIdle()

            assertTrue(tracker.downloadTracked)
            assertEquals("download-location-p-prof-dl", tracker.trackedUrl)
        }

    private class FakeProfileRepository(
        private val user: User? = null,
        private val userPhotos: List<Photo> = emptyList(),
        private val userLikes: List<Photo> = emptyList(),
        private val userCollections: List<PhotoCollection> = emptyList(),
        private val userStats: UserStats? = null,
        private val shouldFail: Boolean = false,
    ) : ProfileRepository {
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
