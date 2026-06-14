package com.example.imagefeed.presentation

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UnifiedSearchPresenterTest {
    @Test
    fun updateQueryPerformsDebouncedSearch() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository =
                FakeUnsplashRepository(
                    photosResponse =
                        SearchResponse(
                            total = 1,
                            totalPages = 1,
                            results = listOf(photo("p-search-1")),
                        ),
                )

            val presenter = UnifiedSearchPresenter(repository, CoroutineScope(dispatcher + SupervisorJob()))
            presenter.updateQuery("forest")

            // Advance time partially, but not past the debounce delay (300ms)
            testScheduler.advanceTimeBy(100)
            assertTrue(
                presenter.state.value.photos
                    .isEmpty(),
            )

            // Advance past debounce delay
            testScheduler.advanceTimeBy(250)
            advanceUntilIdle()

            assertEquals("forest", presenter.state.value.query)
            assertEquals(
                listOf("p-search-1"),
                presenter.state.value.photos
                    .map { it.id },
            )
        }

    @Test
    fun switchingTabsTriggersCorrespondingSearch() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository =
                FakeUnsplashRepository(
                    photosResponse = SearchResponse(1, 1, listOf(photo("p-1"))),
                    collectionsResponse = SearchResponse(1, 1, listOf(collectionSummary("c-1"))),
                )

            val presenter = UnifiedSearchPresenter(repository, CoroutineScope(dispatcher + SupervisorJob()))
            presenter.updateQuery("nature")
            testScheduler.advanceTimeBy(350)
            advanceUntilIdle()

            assertEquals(SearchTab.PHOTOS, presenter.state.value.activeTab)
            assertEquals(1, presenter.state.value.photos.size)

            presenter.setTab(SearchTab.COLLECTIONS)
            advanceUntilIdle()

            assertEquals(SearchTab.COLLECTIONS, presenter.state.value.activeTab)
            assertEquals(1, presenter.state.value.collections.size)
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
            links = PhotoLinks("self", "html", "download", "download-location"),
        )

    private fun collectionSummary(id: String) =
        CollectionSummary(
            id = id,
            title = "Collection $id",
            totalPhotos = 1,
            coverPhoto = photo("p-$id"),
            user =
                User(
                    id = "user-$id",
                    username = "user-$id",
                    name = "User $id",
                    profileImage = ProfileImage("small", "medium", "large"),
                    links = UserLinks("self", "html", "photos"),
                ),
        )

    private class FakeUnsplashRepository(
        private val photosResponse: SearchResponse<Photo> = SearchResponse(0, 0, emptyList()),
        private val collectionsResponse: SearchResponse<CollectionSummary> = SearchResponse(0, 0, emptyList()),
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
        ): SearchResponse<Photo> = photosResponse

        override suspend fun searchCollections(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<CollectionSummary> = collectionsResponse

        override suspend fun searchUsers(
            query: String,
            page: Int,
            perPage: Int,
        ): SearchResponse<User> = SearchResponse(0, 0, emptyList())
    }
}
