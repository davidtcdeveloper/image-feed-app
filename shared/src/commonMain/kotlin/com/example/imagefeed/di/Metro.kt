package com.example.imagefeed.di

import com.example.imagefeed.api.UnsplashApiClient
import com.example.imagefeed.api.createUnsplashHttpClient
import com.example.imagefeed.presentation.CollectionDetailPresenter
import com.example.imagefeed.presentation.CollectionsFeedPresenter
import com.example.imagefeed.presentation.FeedPresenter
import com.example.imagefeed.presentation.PhotoDetailsPresenter
import com.example.imagefeed.presentation.RandomPhotoPresenter
import com.example.imagefeed.presentation.UnifiedSearchPresenter
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
}

@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, bindingContainers = [AppModule::class])
interface ApplicationGraph {
    val httpClient: HttpClient
    val apiClient: UnsplashApiClient
    val repository: UnsplashRepository

    val feedPresenter: FeedPresenter
    val randomPhotoPresenter: RandomPhotoPresenter
    val unifiedSearchPresenter: UnifiedSearchPresenter
    val collectionsFeedPresenter: CollectionsFeedPresenter

    val photoDetailsPresenterFactory: PhotoDetailsPresenter.Factory
    val collectionDetailPresenterFactory: CollectionDetailPresenter.Factory
    val userProfilePresenterFactory: UserProfilePresenter.Factory
}

object MetroHelper {
    val graph: ApplicationGraph by lazy {
        createGraph<ApplicationGraph>()
    }

    fun getFeedPresenter(): FeedPresenter = graph.feedPresenter

    fun getPhotoDetailsPresenter(photoId: String): PhotoDetailsPresenter = graph.photoDetailsPresenterFactory.create(photoId)

    fun getRandomPhotoPresenter(): RandomPhotoPresenter = graph.randomPhotoPresenter

    fun getUnifiedSearchPresenter(): UnifiedSearchPresenter = graph.unifiedSearchPresenter

    fun getCollectionsFeedPresenter(): CollectionsFeedPresenter = graph.collectionsFeedPresenter

    fun getCollectionDetailPresenter(collectionId: String): CollectionDetailPresenter =
        graph.collectionDetailPresenterFactory.create(collectionId)

    fun getUserProfilePresenter(username: String): UserProfilePresenter = graph.userProfilePresenterFactory.create(username)
}
