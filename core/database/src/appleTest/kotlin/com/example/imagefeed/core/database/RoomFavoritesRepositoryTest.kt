package com.example.imagefeed.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.imagefeed.model.Exif
import com.example.imagefeed.model.Location
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.Position
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.Tag
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomFavoritesRepositoryTest {
    private lateinit var database: FavoritesDatabase
    private var currentTimestamp = 1000L

    @BeforeTest
    fun setUp() {
        currentTimestamp = 1000L
        database =
            Room
                .inMemoryDatabaseBuilder<FavoritesDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun toggleFavoritePersistsAndRemovesPhotoInRoom() =
        runTest {
            val repository = RoomFavoritesRepository(database.favoritePhotoDao()) { currentTimestamp++ }
            val photo = samplePhoto("room-photo-1")

            assertFalse(repository.isFavorite(photo.id))
            assertFalse(repository.observeIsFavorite(photo.id).first())

            val saved = repository.toggleFavorite(photo)
            assertTrue(saved)
            assertTrue(repository.isFavorite(photo.id))
            assertTrue(repository.observeIsFavorite(photo.id).first { isFav -> isFav })

            val savedPhotos = repository.observeFavoritePhotos().first { list -> list.size == 1 }
            assertEquals(photo, savedPhotos.first())

            val removed = repository.toggleFavorite(photo)
            assertFalse(removed)
            assertFalse(repository.isFavorite(photo.id))
            assertFalse(repository.observeIsFavorite(photo.id).first { isFav -> !isFav })
            assertEquals(emptyList(), repository.observeFavoritePhotos().first { list -> list.isEmpty() })
        }

    @Test
    fun observeFavoritePhotosOrdersNewestFirstAndSupportsRemove() =
        runTest {
            val repository = RoomFavoritesRepository(database.favoritePhotoDao()) { currentTimestamp++ }
            val older = samplePhoto("room-photo-older")
            val newer = samplePhoto("room-photo-newer")

            repository.toggleFavorite(older)
            repository.toggleFavorite(newer)

            val ordered = repository.observeFavoritePhotos().first { list -> list.size == 2 }
            assertEquals(listOf("room-photo-newer", "room-photo-older"), ordered.map { photo -> photo.id })

            repository.removeFavorite("room-photo-newer")
            val remaining = repository.observeFavoritePhotos().first { list -> list.size == 1 }
            assertEquals(listOf("room-photo-older"), remaining.map { photo -> photo.id })
        }

    @Test
    fun getFavoriteByIdReturnsSavedPhotoOrNullAndReFavoritingUpdatesOrder() =
        runTest {
            val repository = RoomFavoritesRepository(database.favoritePhotoDao()) { currentTimestamp++ }
            val first = samplePhoto("room-lookup-1")
            val second = samplePhoto("room-lookup-2")

            assertNull(repository.getFavoriteById(first.id))

            repository.toggleFavorite(first)
            repository.toggleFavorite(second)

            assertEquals(first, repository.getFavoriteById(first.id))
            assertEquals(second, repository.getFavoriteById(second.id))

            // Unfavorite and re-favorite first so its createdAt supersedes second
            assertFalse(repository.toggleFavorite(first))
            assertNull(repository.getFavoriteById(first.id))
            assertTrue(repository.toggleFavorite(first))

            val reordered = repository.observeFavoritePhotos().first { list -> list.firstOrNull()?.id == first.id }
            assertEquals(listOf("room-lookup-1", "room-lookup-2"), reordered.map { photo -> photo.id })
        }

    private fun samplePhoto(id: String) =
        Photo(
            id = id,
            createdAt = "2026-10-01T12:00:00Z",
            width = 1200,
            height = 1800,
            color = "#1E1E24",
            blurHash = "L6PZfSi_.AyE_3t7t7R**0o#DgR4",
            description = "Sample caption $id",
            altDescription = "Alt $id",
            urls =
                PhotoUrls(
                    raw = "https://images.unsplash.com/$id?ixid=abc",
                    full = "https://images.unsplash.com/$id?w=2400&ixid=abc",
                    regular = "https://images.unsplash.com/$id?w=1080&ixid=abc",
                    small = "https://images.unsplash.com/$id?w=400&ixid=abc",
                    thumb = "https://images.unsplash.com/$id?w=200&ixid=abc",
                ),
            user =
                User(
                    id = "user-$id",
                    username = "photographer_$id",
                    name = "Photographer $id",
                    profileImage = ProfileImage("small", "medium", "large"),
                    links = UserLinks("self", "html", "photos"),
                ),
            links = PhotoLinks("self", "html", "download", "download-location-$id"),
            exif = Exif("Canon", "EOS R5", "1/200", "2.8", "50.0", 100),
            location = Location("Lisbon, Portugal", "Lisbon", "Portugal", Position(38.7223, -9.1393)),
            tags = listOf(Tag("nature")),
        )
}
