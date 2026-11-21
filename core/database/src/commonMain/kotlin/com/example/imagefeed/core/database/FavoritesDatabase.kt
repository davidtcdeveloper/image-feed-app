package com.example.imagefeed.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

internal const val FAVORITES_DB_NAME = "imagefeed_favorites.db"

@Database(entities = [FavoritePhotoEntity::class], version = 1)
@ConstructedBy(FavoritesDatabaseConstructor::class)
internal abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoritePhotoDao(): FavoritePhotoDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object FavoritesDatabaseConstructor : RoomDatabaseConstructor<FavoritesDatabase> {
    override fun initialize(): FavoritesDatabase
}

internal expect fun createFavoritesDatabaseOrNull(): FavoritesDatabase?

internal expect fun currentTimeMillis(): Long
