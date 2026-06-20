package com.example.imagefeed.presentation

import com.example.imagefeed.repository.UnsplashRepository

interface FeedPresenterFactory {
    fun create(): FeedPresenter
}

class DefaultFeedPresenterFactory(
    private val repository: UnsplashRepository,
    private val presenterScopeFactory: PresenterScopeFactory,
) : FeedPresenterFactory {
    override fun create(): FeedPresenter = FeedPresenter(repository, presenterScopeFactory)
}

interface CollectionsFeedPresenterFactory {
    fun create(): CollectionsFeedPresenter
}

class DefaultCollectionsFeedPresenterFactory(
    private val repository: UnsplashRepository,
    private val presenterScopeFactory: PresenterScopeFactory,
) : CollectionsFeedPresenterFactory {
    override fun create(): CollectionsFeedPresenter = CollectionsFeedPresenter(repository, presenterScopeFactory)
}

interface UnifiedSearchPresenterFactory {
    fun create(): UnifiedSearchPresenter
}

class DefaultUnifiedSearchPresenterFactory(
    private val repository: UnsplashRepository,
    private val presenterScopeFactory: PresenterScopeFactory,
) : UnifiedSearchPresenterFactory {
    override fun create(): UnifiedSearchPresenter = UnifiedSearchPresenter(repository, presenterScopeFactory)
}

interface RandomPhotoPresenterFactory {
    fun create(): RandomPhotoPresenter
}

class DefaultRandomPhotoPresenterFactory(
    private val repository: UnsplashRepository,
    private val presenterScopeFactory: PresenterScopeFactory,
) : RandomPhotoPresenterFactory {
    override fun create(): RandomPhotoPresenter = RandomPhotoPresenter(repository, presenterScopeFactory)
}
