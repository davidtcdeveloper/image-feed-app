package com.example.imagefeed.presentation

import com.example.imagefeed.model.CollectionLinks
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailPresenterTest {
    @Test
    fun loadsCollectionHeaderPhotosAndRelatedSuccessfully() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val user =
                User(
                    id = "user-1",
                    username = "user-1",
                    name = "User One",
                    profileImage = ProfileImage("small", "medium", "large"),
                    links = UserLinks("self", "html", "photos"),
                )
            val testCollection =
                PhotoCollection(
                    id = "col-1",
                    title = "Test Collection",
                    totalPhotos = 10,
                    user = user,
                    links = CollectionLinks("self", "html", "photos"),
                )
            val testPhotos = listOf(photo("p-col-1"), photo("p-col-2"))
            val testRelated =
                listOf(
                    PhotoCollection(
                        id = "col-related-1",
                        title = "Related Collection",
                        totalPhotos = 3,
                        user = user,
                        links = CollectionLinks("self", "html", "photos"),
                    ),
                )

            val repository =
                FakeUnsplashRepository(
                    collection = testCollection,
                    photos = testPhotos,
                    related = testRelated,
                )

            val presenter =
                CollectionDetailPresenter(
                    repository = repository,
                    collectionId = "col-1",
                    presenterScope = CoroutineScope(dispatcher + SupervisorJob()),
                )

            advanceUntilIdle()

            assertNotNull(presenter.state.value.collection)
            assertEquals(
                "Test Collection",
                presenter.state.value.collection
                    ?.title,
            )
            assertEquals(2, presenter.state.value.photos.size)
            assertEquals(1, presenter.state.value.related.size)
            assertEquals(
                "Related Collection",
                presenter.state.value.related[0]
                    .title,
            )
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
        private val collection: PhotoCollection,
        private val photos: List<Photo>,
        private val related: List<PhotoCollection>,
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

        override suspend fun getCollection(id: String): PhotoCollection = collection

        override suspend fun getCollectionPhotos(
            id: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = photos

        override suspend fun getRelatedCollections(id: String): List<PhotoCollection> = related

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
