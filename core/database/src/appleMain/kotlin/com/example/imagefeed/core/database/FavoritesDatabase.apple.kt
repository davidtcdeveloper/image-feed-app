package com.example.imagefeed.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
private fun appleDatabasePath(bundleId: String): String {
    val fileManager = NSFileManager.defaultManager
    val appSupportUrl =
        fileManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    val baseDir = appSupportUrl?.path ?: NSHomeDirectory()
    val appDir = "$baseDir/$bundleId"
    fileManager.createDirectoryAtPath(
        path = appDir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return "$appDir/$FAVORITES_DB_NAME"
}

internal actual fun createFavoritesDatabaseOrNull(): FavoritesDatabase? {
    // Bare Kotlin/Native test executables have no bundle identifier; fall back to in-memory storage.
    val bundleId = NSBundle.mainBundle.bundleIdentifier?.takeIf { it.isNotBlank() } ?: return null
    return Room
        .databaseBuilder<FavoritesDatabase>(
            name = appleDatabasePath(bundleId),
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
