package com.example.imagefeed.di

import com.example.imagefeed.api.UnsplashApiClient
import com.example.imagefeed.api.createUnsplashHttpClient
import com.example.imagefeed.presentation.CollectionDetailPresenter
import com.example.imagefeed.presentation.CollectionsFeedPresenter
import com.example.imagefeed.presentation.CollectionsFeedPresenterFactory
import com.example.imagefeed.presentation.DefaultCollectionsFeedPresenterFactory
import com.example.imagefeed.presentation.DefaultDispatcherProvider
import com.example.imagefeed.presentation.DefaultFeedPresenterFactory
import com.example.imagefeed.presentation.DefaultPresenterScopeFactory
import com.example.imagefeed.presentation.DefaultRandomPhotoPresenterFactory
import com.example.imagefeed.presentation.DefaultUnifiedSearchPresenterFactory
import com.example.imagefeed.presentation.DispatcherProvider
import com.example.imagefeed.presentation.FeedPresenter
import com.example.imagefeed.presentation.FeedPresenterFactory
import com.example.imagefeed.presentation.PhotoDetailsPresenter
import com.example.imagefeed.presentation.PresenterScopeFactory
import com.example.imagefeed.presentation.RandomPhotoPresenter
import com.example.imagefeed.presentation.RandomPhotoPresenterFactory
import com.example.imagefeed.presentation.UnifiedSearchPresenter
import com.example.imagefeed.presentation.UnifiedSearchPresenterFactory
import com.example.imagefeed.presentation.UserProfilePresenter
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.repository.UnsplashRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Scope
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.ktor.client.HttpClient

@Scope
annotation class AppScope

@BindingContainer
object AppModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(): HttpClient = createUnsplashHttpClient()

    @Provides
    @SingleIn(AppScope::class)
    fun provideApiClient(client: HttpClient): UnsplashApiClient = UnsplashApiClient(client)

    @Provides
    @SingleIn(AppScope::class)
    fun provideRepository(apiClient: UnsplashApiClient): UnsplashRepository = UnsplashRepositoryImpl(apiClient)

    @Provides
    @SingleIn(AppScope::class)
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @SingleIn(AppScope::class)
    fun providePresenterScopeFactory(dispatcherProvider: DispatcherProvider): PresenterScopeFactory =
        DefaultPresenterScopeFactory(dispatcherProvider)

    @Provides
    fun provideFeedPresenterFactory(
        repository: UnsplashRepository,
        presenterScopeFactory: PresenterScopeFactory,
    ): FeedPresenterFactory = DefaultFeedPresenterFactory(repository, presenterScopeFactory)

    @Provides
    fun provideRandomPhotoPresenterFactory(
        repository: UnsplashRepository,
        presenterScopeFactory: PresenterScopeFactory,
    ): RandomPhotoPresenterFactory = DefaultRandomPhotoPresenterFactory(repository, presenterScopeFactory)

    @Provides
    fun provideUnifiedSearchPresenterFactory(
        repository: UnsplashRepository,
        presenterScopeFactory: PresenterScopeFactory,
    ): UnifiedSearchPresenterFactory = DefaultUnifiedSearchPresenterFactory(repository, presenterScopeFactory)

    @Provides
    fun provideCollectionsFeedPresenterFactory(
        repository: UnsplashRepository,
        presenterScopeFactory: PresenterScopeFactory,
    ): CollectionsFeedPresenterFactory = DefaultCollectionsFeedPresenterFactory(repository, presenterScopeFactory)
}

@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, bindingContainers = [AppModule::class])
interface ApplicationGraph {
    val httpClient: HttpClient
    val apiClient: UnsplashApiClient
    val repository: UnsplashRepository

    val feedPresenterFactory: FeedPresenterFactory
    val randomPhotoPresenterFactory: RandomPhotoPresenterFactory
    val unifiedSearchPresenterFactory: UnifiedSearchPresenterFactory
    val collectionsFeedPresenterFactory: CollectionsFeedPresenterFactory

    val photoDetailsPresenterFactory: PhotoDetailsPresenter.Factory
    val collectionDetailPresenterFactory: CollectionDetailPresenter.Factory
    val userProfilePresenterFactory: UserProfilePresenter.Factory
}

object MetroHelper {
    val graph: ApplicationGraph by lazy {
        createGraph<ApplicationGraph>()
    }

    fun getFeedPresenter(): FeedPresenter = graph.feedPresenterFactory.create()

    fun getPhotoDetailsPresenter(photoId: String): PhotoDetailsPresenter = graph.photoDetailsPresenterFactory.create(photoId)

    fun getRandomPhotoPresenter(): RandomPhotoPresenter = graph.randomPhotoPresenterFactory.create()

    fun getUnifiedSearchPresenter(): UnifiedSearchPresenter = graph.unifiedSearchPresenterFactory.create()

    fun getCollectionsFeedPresenter(): CollectionsFeedPresenter = graph.collectionsFeedPresenterFactory.create()

    fun getCollectionDetailPresenter(collectionId: String): CollectionDetailPresenter =
        graph.collectionDetailPresenterFactory.create(collectionId)

    fun getUserProfilePresenter(username: String): UserProfilePresenter = graph.userProfilePresenterFactory.create(username)
}
