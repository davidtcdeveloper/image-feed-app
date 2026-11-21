package com.example.imagefeed.core.database.di

import com.example.imagefeed.core.database.FavoritesRepository
import com.example.imagefeed.core.database.InMemoryFavoritesRepository
import com.example.imagefeed.core.database.RoomFavoritesRepository
import com.example.imagefeed.core.database.createFavoritesDatabaseOrNull
import com.example.imagefeed.core.network.di.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
@BindingContainer
object DatabaseModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideFavoritesRepository(): FavoritesRepository {
        val database = createFavoritesDatabaseOrNull()
        return if (database != null) {
            RoomFavoritesRepository(database.favoritePhotoDao())
        } else {
            InMemoryFavoritesRepository()
        }
    }
}
