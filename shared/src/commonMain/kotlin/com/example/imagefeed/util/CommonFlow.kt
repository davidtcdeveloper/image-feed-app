package com.example.imagefeed.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface Closeable {
    fun close()
}

class CommonFlow<T>(
    private val origin: Flow<T>,
) : Flow<T> by origin {
    fun watch(block: (T) -> Unit): Closeable {
        val job = Job()
        // Run on Main Dispatcher so updates are delivered on the main thread to iOS UI
        val scope = CoroutineScope(Dispatchers.Main + job)

        origin
            .onEach { block(it) }
            .launchIn(scope)

        return object : Closeable {
            override fun close() {
                job.cancel()
            }
        }
    }
}
