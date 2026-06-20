package com.example.imagefeed.presentation

import kotlinx.coroutines.CoroutineDispatcher

class TestDispatcherProvider(
    private val dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
}

class TestPresenterScopeFactory(
    private val dispatcher: CoroutineDispatcher,
) : PresenterScopeFactory {
    override fun create(): PresenterScope = PresenterScope(TestDispatcherProvider(dispatcher))
}
