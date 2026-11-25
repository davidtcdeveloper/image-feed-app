package com.example.imagefeed.model.cache

internal actual fun createCacheLock(): CacheLock =
    object : CacheLock {
        override fun <T> withLock(block: () -> T): T = synchronized(this) { block() }
    }
