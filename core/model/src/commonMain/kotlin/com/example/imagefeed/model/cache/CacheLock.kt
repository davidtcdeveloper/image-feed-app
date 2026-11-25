package com.example.imagefeed.model.cache

internal interface CacheLock {
    fun <T> withLock(block: () -> T): T
}

internal expect fun createCacheLock(): CacheLock
