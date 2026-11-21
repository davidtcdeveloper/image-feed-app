package com.example.imagefeed.core.database

import com.example.imagefeed.model.Photo
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeIsFavorite(photoId: String): Flow<Boolean>

    fun observeFavoritePhotos(): Flow<List<Photo>>

    suspend fun getFavoriteById(photoId: String): Photo?

    suspend fun isFavorite(photoId: String): Boolean

    suspend fun toggleFavorite(photo: Photo): Boolean

    suspend fun removeFavorite(photoId: String)
}
