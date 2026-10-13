package com.example.imagefeed.presentation.search

import com.example.imagefeed.model.CollectionSummary
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UnifiedSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun user(id: String) =
        User(
            id = "user-$id",
            username = "user-$id",
            name = "User $id",
            profileImage = ProfileImage("small", "medium", "large"),
            links = UserLinks("self", "html", "photos"),
        )

    private fun photo(id: String) =
        Photo(
            id = id,
            width = 100,
            height = 100,
            urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
            user = user("u1"),
            links = PhotoLinks("self", "html", "download", "download-location-$id"),
        )

    @Test
    fun debouncesQueryAndExecutesSearch() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    photoResults = listOf(photo("p-search-1")),
                )
            val viewModel = UnifiedSearchViewModel(repository)

            viewModel.dispatch(UnifiedSearchIntent.UpdateQuery("nature"))
            advanceTimeBy(100)
            assertEquals(0, viewModel.state.value.photos.size)

            advanceTimeBy(250)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.photos.size)
            assertEquals(
                "p-search-1",
                viewModel.state.value.photos[0]
                    .id,
            )
        }

    @Test
    fun blankQueryClearsResults() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    photoResults = listOf(photo("p-search-1")),
                )
            val viewModel = UnifiedSearchViewModel(repository)

            viewModel.dispatch(UnifiedSearchIntent.SubmitSearch("nature"))
            advanceUntilIdle()
            assertEquals(1, viewModel.state.value.photos.size)

            viewModel.dispatch(UnifiedSearchIntent.UpdateQuery(""))
            advanceUntilIdle()
            assertEquals(0, viewModel.state.value.photos.size)
            assertEquals("", viewModel.state.value.query)
        }

    @Test
    fun managesSearchHistory() =
        runTest(testDispatcher) {
            val repository = FakeUnsplashRepository()
            val viewModel = UnifiedSearchViewModel(repository)

            viewModel.dispatch(UnifiedSearchIntent.AddHistoryEntry("mountains"))
            advanceUntilIdle()
            assertEquals(
                "mountains",
                viewModel.state.value.searchHistory
                    .first(),
            )

            viewModel.dispatch(UnifiedSearchIntent.RemoveHistoryEntry("mountains"))
            advanceUntilIdle()
            assertTrue(
                viewModel.state.value.searchHistory
                    .none { it == "mountains" },
            )

            viewModel.dispatch(UnifiedSearchIntent.ClearHistory)
            advanceUntilIdle()
            assertTrue(
                viewModel.state.value.searchHistory
                    .isEmpty(),
            )
        }

    @Test
    fun submitSearchDismissesSearchAndPerformsImmediateSearch() =
        runTest(testDispatcher) {
            val repository =
                FakeUnsplashRepository(
                    photoResults = listOf(photo("p-immediate")),
                )
            val viewModel = UnifiedSearchViewModel(repository)

            viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(true))
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isSearchActive)

            viewModel.dispatch(UnifiedSearchIntent.SubmitSearch("cars"))
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isSearchActive)
            assertEquals(1, viewModel.state.value.photos.size)
            assertEquals(
                "cars",
                viewModel.state.value.searchHistory
                    .first(),
            )
        }

    private class FakeUnsplashRepository(
        private val photoResults: List<Photo> = emptyList(),
        private val collectionResults: List<CollectionSummary> = emptyList(),
        private val userResults: List<User> = emptyList(),
    ) : UnsplashRepository {
        override suspend fun getPhotos(
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getPhotoDetails(id: String): Photo = error("Not used")

        override suspend fun getPhotoStats(id: String): PhotoStats = error("Not used")

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
        ): SearchResponse<Photo> =
            SearchResponse(
                total = photoResults.size,
                totalPages = 1,
                results = photoResults,
            )

        override suspend fun searchCollections(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<CollectionSummary> =
            SearchResponse(
                total = collectionResults.size,
                totalPages = 1,
                results = collectionResults,
            )

        override suspend fun searchUsers(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<User> =
            SearchResponse(
                total = userResults.size,
                totalPages = 1,
                results = userResults,
            )
    }
}
