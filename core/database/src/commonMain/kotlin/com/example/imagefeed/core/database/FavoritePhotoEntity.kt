package com.example.imagefeed.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_photos")
internal data class FavoritePhotoEntity(
    @PrimaryKey val id: String,
    val photoJson: String,
    val createdAt: Long,
)
