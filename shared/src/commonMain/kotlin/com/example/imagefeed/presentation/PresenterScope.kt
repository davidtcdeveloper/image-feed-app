package com.example.imagefeed.presentation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

interface DispatcherProvider {
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
}

interface PresenterScopeFactory {
    fun create(): PresenterScope
}

class PresenterScope(
    dispatcherProvider: DispatcherProvider,
    private val parentJob: Job = SupervisorJob(),
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = parentJob + dispatcherProvider.main

    fun clear() {
        parentJob.cancel()
    }
}

fun CoroutineContext.isActive(): Boolean = this[Job]?.isActive == true

fun <T> MutableStateFlow<T>.updateIfActive(
    coroutineContext: CoroutineContext,
    transform: (T) -> T,
): Boolean {
    if (!coroutineContext.isActive()) return false
    update(transform)
    return true
}

class DefaultPresenterScopeFactory(
    private val dispatcherProvider: DispatcherProvider,
) : PresenterScopeFactory {
    override fun create(): PresenterScope = PresenterScope(dispatcherProvider)
}
