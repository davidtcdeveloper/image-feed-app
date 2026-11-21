package com.example.imagefeed.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FavoritePhotoDao {
    @Query("SELECT * FROM favorite_photos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoritePhotoEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_photos WHERE id = :photoId)")
    fun observeIsFavorite(photoId: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_photos WHERE id = :photoId LIMIT 1")
    suspend fun getById(photoId: String): FavoritePhotoEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_photos WHERE id = :photoId)")
    suspend fun isFavorite(photoId: String): Boolean

    @Upsert
    suspend fun upsert(entity: FavoritePhotoEntity)

    @Query("DELETE FROM favorite_photos WHERE id = :photoId")
    suspend fun deleteById(photoId: String)
}
