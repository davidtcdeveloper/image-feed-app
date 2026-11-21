package com.example.imagefeed.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

object DatabaseContextHolder {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun getOrNull(): Context? = appContext
}

internal actual fun createFavoritesDatabaseOrNull(): FavoritesDatabase? {
    val context = DatabaseContextHolder.getOrNull() ?: return null
    val dbFile = context.getDatabasePath(FAVORITES_DB_NAME)
    return Room
        .databaseBuilder<FavoritesDatabase>(
            context = context,
            name = dbFile.absolutePath,
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
