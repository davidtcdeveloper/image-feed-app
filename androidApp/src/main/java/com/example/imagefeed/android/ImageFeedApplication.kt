package com.example.imagefeed.android

import android.app.Application
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.allowRgb565
import coil3.request.crossfade
import com.example.imagefeed.android.util.BlurHashDecoder

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
                    .maxSizePercent(context, 0.15)
                    .strongReferencesEnabled(true)
                    .build()
            }.allowRgb565(true)
            .crossfade(200)
            .build()

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val imageLoader = SingletonImageLoader.get(this)
        val memoryCache = imageLoader.memoryCache
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        ) {
            memoryCache?.clear()
            BlurHashDecoder.clearCache()
        } else if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
        ) {
            memoryCache?.let { it.trimToSize(it.size / 2) }
            BlurHashDecoder.clearCache()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        SingletonImageLoader.get(this).memoryCache?.clear()
        BlurHashDecoder.clearCache()
    }
}
