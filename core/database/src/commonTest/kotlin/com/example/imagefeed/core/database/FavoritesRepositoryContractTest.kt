package com.example.imagefeed.core.database

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FavoritesRepositoryContractTest {
    @Test
    fun inMemoryFavoritesRepositoryTogglesOrdersLookupsAndRemoves() =
        runTest {
            var now = 5000L
            val repository = InMemoryFavoritesRepository { now++ }
            val first = samplePhoto("contract-1")
            val second = samplePhoto("contract-2")

            assertFalse(repository.isFavorite(first.id))
            assertNull(repository.getFavoriteById(first.id))
            assertTrue(repository.toggleFavorite(first))
            assertTrue(repository.isFavorite(first.id))
            assertEquals(first, repository.getFavoriteById(first.id))
            assertTrue(repository.observeIsFavorite(first.id).first())

            assertTrue(repository.toggleFavorite(second))
            val allPhotos = repository.observeFavoritePhotos().first()
            assertEquals(listOf("contract-2", "contract-1"), allPhotos.map { photo -> photo.id })

            repository.removeFavorite(second.id)
            assertFalse(repository.isFavorite(second.id))
            assertNull(repository.getFavoriteById(second.id))
            assertEquals(listOf("contract-1"), repository.observeFavoritePhotos().first().map { photo -> photo.id })

            assertFalse(repository.toggleFavorite(first))
            assertNull(repository.getFavoriteById(first.id))
            assertEquals(emptyList(), repository.observeFavoritePhotos().first())
        }

    @Test
    fun roomFavoritesRepositorySkipsCorruptedRowsAndReturnsNullLookup() =
        runTest {
            val validPhoto = samplePhoto("valid-1")
            val dao = FakeFavoritePhotoDao()
            val seedRepo = RoomFavoritesRepository(dao) { 1000L }
            seedRepo.toggleFavorite(validPhoto)
            dao.upsert(FavoritePhotoEntity(id = "corrupt-1", photoJson = "{not-valid-json", createdAt = 2000L))

            val favorites = seedRepo.observeFavoritePhotos().first()
            assertEquals(listOf(validPhoto), favorites)
            assertEquals(validPhoto, seedRepo.getFavoriteById(validPhoto.id))
            assertNull(seedRepo.getFavoriteById("corrupt-1"))
            assertNull(seedRepo.getFavoriteById("missing-1"))
        }

    @Test
    fun roomFavoritesRepositoryReFavoritingUpdatesCreatedAtOrdering() =
        runTest {
            var now = 1000L
            val dao = FakeFavoritePhotoDao()
            val repository = RoomFavoritesRepository(dao) { now++ }
            val first = samplePhoto("re-1")
            val second = samplePhoto("re-2")

            assertTrue(repository.toggleFavorite(first))
            assertTrue(repository.toggleFavorite(second))
            assertEquals(listOf("re-2", "re-1"), repository.observeFavoritePhotos().first().map { it.id })

            // Remove first and re-favorite it later so it moves to the top
            assertFalse(repository.toggleFavorite(first))
            assertTrue(repository.toggleFavorite(first))
            assertEquals(listOf("re-1", "re-2"), repository.observeFavoritePhotos().first().map { it.id })
        }

    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun roomFavoritesRepositoryObserveIsFavoriteDeduplicatesEqualEmissions() =
        runTest {
            val emissions = MutableSharedFlow<Boolean>(replay = 0)
            val dao = FakeFavoritePhotoDao(customFavoriteFlow = emissions)
            val repository = RoomFavoritesRepository(dao)
            val collected = mutableListOf<Boolean>()

            val job =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    repository.observeIsFavorite("dup-1").take(3).toList(collected)
                }

            emissions.emit(false)
            emissions.emit(false)
            emissions.emit(true)
            emissions.emit(true)
            emissions.emit(false)
            job.join()

            assertEquals(listOf(false, true, false), collected)
        }

    private class FakeFavoritePhotoDao(
        private val customFavoriteFlow: Flow<Boolean>? = null,
    ) : FavoritePhotoDao {
        private val rows = MutableStateFlow<List<FavoritePhotoEntity>>(emptyList())

        override fun observeAll(): Flow<List<FavoritePhotoEntity>> =
            rows.map { list -> list.sortedByDescending { entity -> entity.createdAt } }

        override fun observeIsFavorite(photoId: String): Flow<Boolean> =
            customFavoriteFlow ?: rows.map { list -> list.any { entity -> entity.id == photoId } }

        override suspend fun getById(photoId: String): FavoritePhotoEntity? = rows.value.firstOrNull { entity -> entity.id == photoId }

        override suspend fun isFavorite(photoId: String): Boolean = rows.value.any { entity -> entity.id == photoId }

        override suspend fun upsert(entity: FavoritePhotoEntity) {
            rows.update { list -> list.filterNot { it.id == entity.id } + entity }
        }

        override suspend fun deleteById(photoId: String) {
            rows.update { list -> list.filterNot { it.id == photoId } }
        }
    }

    private fun samplePhoto(id: String) =
        Photo(
            id = id,
            width = 1000,
            height = 1500,
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
}
