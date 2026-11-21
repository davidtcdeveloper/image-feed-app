package com.example.imagefeed.core.database

import com.example.imagefeed.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val favoritesJson =
    Json {
        ignoreUnknownKeys = true
    }

internal class RoomFavoritesRepository(
    private val dao: FavoritePhotoDao,
    private val clock: () -> Long = ::currentTimeMillis,
) : FavoritesRepository {
    private val writeMutex = Mutex()

    override fun observeIsFavorite(photoId: String): Flow<Boolean> = dao.observeIsFavorite(photoId).distinctUntilChanged()

    override fun observeFavoritePhotos(): Flow<List<Photo>> =
        dao
            .observeAll()
            .map { entities ->
                entities.mapNotNull { entity -> decodePhotoOrNull(entity.photoJson) }
            }.flowOn(Dispatchers.Default)

    override suspend fun getFavoriteById(photoId: String): Photo? {
        val entity = dao.getById(photoId) ?: return null
        return withContext(Dispatchers.Default) {
            decodePhotoOrNull(entity.photoJson)
        }
    }

    override suspend fun isFavorite(photoId: String): Boolean = dao.isFavorite(photoId)

    override suspend fun toggleFavorite(photo: Photo): Boolean =
        writeMutex.withLock {
            if (dao.isFavorite(photo.id)) {
                dao.deleteById(photo.id)
                false
            } else {
                val encodedJson =
                    withContext(Dispatchers.Default) {
                        favoritesJson.encodeToString(photo)
                    }
                val entity =
                    FavoritePhotoEntity(
                        id = photo.id,
                        photoJson = encodedJson,
                        createdAt = clock(),
                    )
                dao.upsert(entity)
                true
            }
        }

    override suspend fun removeFavorite(photoId: String) {
        writeMutex.withLock {
            dao.deleteById(photoId)
        }
    }
}

internal class InMemoryFavoritesRepository(
    private val clock: () -> Long = ::currentTimeMillis,
) : FavoritesRepository {
    private val writeMutex = Mutex()
    private val entries = MutableStateFlow<List<FavoritePhotoEntity>>(emptyList())

    override fun observeIsFavorite(photoId: String): Flow<Boolean> =
        entries
            .map { list -> list.any { entity -> entity.id == photoId } }
            .distinctUntilChanged()

    override fun observeFavoritePhotos(): Flow<List<Photo>> =
        entries.map { list ->
            list
                .sortedByDescending { entity -> entity.createdAt }
                .mapNotNull { entity -> decodePhotoOrNull(entity.photoJson) }
        }

    override suspend fun getFavoriteById(photoId: String): Photo? =
        entries.value
            .firstOrNull { entity -> entity.id == photoId }
            ?.let { entity -> decodePhotoOrNull(entity.photoJson) }

    override suspend fun isFavorite(photoId: String): Boolean = entries.value.any { entity -> entity.id == photoId }

    override suspend fun toggleFavorite(photo: Photo): Boolean =
        writeMutex.withLock {
            val currentlySaved = entries.value.any { entity -> entity.id == photo.id }
            if (currentlySaved) {
                entries.update { list -> list.filterNot { entity -> entity.id == photo.id } }
                false
            } else {
                val entity =
                    FavoritePhotoEntity(
                        id = photo.id,
                        photoJson = favoritesJson.encodeToString(photo),
                        createdAt = clock(),
                    )
                entries.update { list ->
                    list.filterNot { existing -> existing.id == photo.id } + entity
                }
                true
            }
        }

    override suspend fun removeFavorite(photoId: String) {
        writeMutex.withLock {
            entries.update { list -> list.filterNot { entity -> entity.id == photoId } }
        }
    }
}

private fun decodePhotoOrNull(rawJson: String): Photo? =
    try {
        favoritesJson.decodeFromString<Photo>(rawJson)
    } catch (_: SerializationException) {
        null
    }
