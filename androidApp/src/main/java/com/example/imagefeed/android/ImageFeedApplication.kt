package com.example.imagefeed.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toPath

/**
 * Application entry point for the Android client.
 *
 * Implements [SingletonImageLoader.Factory] directly rather than injecting via Metro DI:
 * Coil is an Android UI concern (not used on iOS/shared code) and requires an Android [PlatformContext].
 * Implementing the factory on [Application] enables lazy on-demand initialization on the first image request
 * without eager startup overhead or coupling shared Metro graphs to Android UI image pipelines.
 */
class ImageFeedApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(
                        context.cacheDir
                            .resolve("image_cache")
                            .absolutePath
                            .toPath(),
                    ).maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }.crossfade(200)
            .build()
}
