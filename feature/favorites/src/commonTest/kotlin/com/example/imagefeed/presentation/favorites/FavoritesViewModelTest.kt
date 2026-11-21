package com.example.imagefeed.presentation.favorites

import com.example.imagefeed.core.database.FavoritesRepository
import com.example.imagefeed.model.AppError
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
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
    fun initialEmptyStateTransitionsLoadingToFalse() =
        runTest(testDispatcher) {
            val repository = FakeFavoritesRepository()
            val viewModel = FavoritesViewModel(repository)

            assertTrue(viewModel.state.value.isLoading)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertEquals(emptyList(), viewModel.state.value.photos)
            viewModel.clear()
        }

    @Test
    fun observesFavoritesReactivelyAndHandlesRemoval() =
        runTest(testDispatcher) {
            val first = samplePhoto("fav-1")
            val second = samplePhoto("fav-2")
            val repository = FakeFavoritesRepository(listOf(second, first))

            val viewModel = FavoritesViewModel(repository)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertEquals(
                listOf("fav-2", "fav-1"),
                viewModel.state.value.photos
                    .map { photo -> photo.id },
            )

            viewModel.dispatch(FavoritesIntent.RemoveFavorite("fav-2"))
            advanceUntilIdle()

            assertEquals(
                listOf("fav-1"),
                viewModel.state.value.photos
                    .map { photo -> photo.id },
            )
            assertEquals(FavoritesEffect.ShowFavoriteRemovedSnackbar, viewModel.effects.first())

            val third = samplePhoto("fav-3")
            repository.toggleFavorite(third)
            advanceUntilIdle()

            assertEquals(
                listOf("fav-3", "fav-1"),
                viewModel.state.value.photos
                    .map { photo -> photo.id },
            )
            viewModel.clear()
        }

    @Test
    fun clearCancelsObservationDeterministically() =
        runTest(testDispatcher) {
            val first = samplePhoto("fav-clear-1")
            val second = samplePhoto("fav-clear-2")
            val repository = FakeFavoritesRepository(listOf(first))
            val viewModel = FavoritesViewModel(repository)
            advanceUntilIdle()

            assertEquals(
                listOf("fav-clear-1"),
                viewModel.state.value.photos
                    .map { it.id },
            )

            viewModel.clear()
            repository.toggleFavorite(second)
            advanceUntilIdle()

            assertEquals(
                listOf("fav-clear-1"),
                viewModel.state.value.photos
                    .map { it.id },
            )
        }

    @Test
    fun storageErrorsEmitShowErrorSnackbarEffect() =
        runTest(testDispatcher) {
            val photo = samplePhoto("fav-err")
            val repository = FakeFavoritesRepository(listOf(photo)).apply { shouldFailWrite = true }
            val viewModel = FavoritesViewModel(repository)
            advanceUntilIdle()

            viewModel.dispatch(FavoritesIntent.RemoveFavorite(photo.id))
            advanceUntilIdle()

            val writeEffect = viewModel.effects.first()
            assertEquals(FavoritesEffect.ShowErrorSnackbar(AppError.Network), writeEffect)
            viewModel.clear()

            val failingObserveRepo = FakeFavoritesRepository().apply { shouldFailObserve = true }
            val failingViewModel = FavoritesViewModel(failingObserveRepo)
            advanceUntilIdle()

            assertFalse(failingViewModel.state.value.isLoading)
            val observeEffect = failingViewModel.effects.first()
            assertEquals(FavoritesEffect.ShowErrorSnackbar(AppError.Network), observeEffect)
            failingViewModel.clear()
        }

    private fun samplePhoto(id: String) =
        Photo(
            id = id,
            width = 800,
            height = 1200,
            urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
            user =
                User(
                    id = "user-$id",
                    username = "user_$id",
                    name = "User $id",
                    profileImage = ProfileImage("small", "medium", "large"),
                    links = UserLinks("self", "html", "photos"),
                ),
            links = PhotoLinks("self", "html", "download", "download-location-$id"),
        )

    private class FakeFavoritesRepository(
        initialPhotos: List<Photo> = emptyList(),
    ) : FavoritesRepository {
        private val savedPhotos = MutableStateFlow(initialPhotos)
        var shouldFailWrite = false
        var shouldFailObserve = false

        override fun observeIsFavorite(photoId: String): Flow<Boolean> =
            savedPhotos.map { list -> list.any { photo -> photo.id == photoId } }

        override fun observeFavoritePhotos(): Flow<List<Photo>> =
            if (shouldFailObserve) {
                flow { throw IllegalStateException("Storage read error") }
            } else {
                savedPhotos
            }

        override suspend fun getFavoriteById(photoId: String): Photo? = savedPhotos.value.firstOrNull { photo -> photo.id == photoId }

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
