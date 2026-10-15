package com.example.imagefeed.presentation.collections

import com.example.imagefeed.collections.data.CollectionsRepository
import com.example.imagefeed.model.CollectionLinks
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
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
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsFeedViewModelTest {
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

    private fun collection(id: String) =
        PhotoCollection(
            id = id,
            title = "Collection $id",
            totalPhotos = 10,
            user = user(),
            links = CollectionLinks("self", "html", "photos"),
        )

    @Test
    fun loadsInitialCollectionsSuccessfully() =
        runTest(testDispatcher) {
            val repository =
                FakeCollectionsRepository(
                    collections = listOf(collection("c-1"), collection("c-2")),
                )
            val viewModel = CollectionsFeedViewModel(repository)
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.collections.size)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun handlesLoadingErrorGracefully() =
        runTest(testDispatcher) {
            val repository = FakeCollectionsRepository(shouldFail = true)
            val viewModel = CollectionsFeedViewModel(repository)
            advanceUntilIdle()

            assertEquals("Network error", viewModel.state.value.error)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun refreshReloadsCollections() =
        runTest(testDispatcher) {
            val repository =
                FakeCollectionsRepository(
                    collections = listOf(collection("c-1")),
                )
            val viewModel = CollectionsFeedViewModel(repository)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.collections.size)

            viewModel.dispatch(CollectionsFeedIntent.Refresh)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.collections.size)
            assertFalse(viewModel.state.value.isRefreshing)
        }

    private class FakeCollectionsRepository(
        private val collections: List<PhotoCollection> = emptyList(),
        private val shouldFail: Boolean = false,
    ) : CollectionsRepository {
        override suspend fun getCollections(
            page: Int,
            perPage: Int,
        ): List<PhotoCollection> {
            if (shouldFail) throw RuntimeException("Network error")
            return collections
        }

        override suspend fun getCollection(id: String): PhotoCollection = error("Not used")

        override suspend fun getCollectionPhotos(
            id: String,
            page: Int,
            perPage: Int,
        ): List<Photo> = emptyList()

        override suspend fun getRelatedCollections(id: String): List<PhotoCollection> = emptyList()
    }
}
