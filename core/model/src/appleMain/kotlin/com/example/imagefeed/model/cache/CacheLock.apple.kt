package com.example.imagefeed.model.cache

import platform.Foundation.NSLock

internal actual fun createCacheLock(): CacheLock =
    object : CacheLock {
        private val lock = NSLock()

        override fun <T> withLock(block: () -> T): T {
            lock.lock()
            return try {
                block()
            } finally {
                lock.unlock()
            }
        }
    }
