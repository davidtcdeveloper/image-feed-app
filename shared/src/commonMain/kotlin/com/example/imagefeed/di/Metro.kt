package com.example.imagefeed.di

import com.example.imagefeed.api.UnsplashApiClient
import com.example.imagefeed.core.network.KtorPhotoDownloadTracker
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.core.network.createUnsplashHttpClient
import com.example.imagefeed.details.di.PhotoDetailsModule
import com.example.imagefeed.presentation.collection.CollectionDetailViewModel
import com.example.imagefeed.presentation.collections.CollectionsFeedViewModel
import com.example.imagefeed.presentation.details.PhotoDetailsViewModel
import com.example.imagefeed.presentation.feed.FeedViewModel
import com.example.imagefeed.presentation.profile.UserProfileViewModel
import com.example.imagefeed.presentation.random.RandomPhotoViewModel
import com.example.imagefeed.presentation.search.UnifiedSearchViewModel
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
    fun providePhotoDownloadTracker(client: HttpClient): PhotoDownloadTracker = KtorPhotoDownloadTracker(client)

    @Provides
    @SingleIn(AppScope::class)
    fun provideApiClient(client: HttpClient): UnsplashApiClient = UnsplashApiClient(client)

    @Provides
    @SingleIn(AppScope::class)
    fun provideRepository(
        apiClient: UnsplashApiClient,
        photoDownloadTracker: PhotoDownloadTracker,
    ): UnsplashRepository = UnsplashRepositoryImpl(apiClient, photoDownloadTracker)
}

@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, bindingContainers = [AppModule::class, PhotoDetailsModule::class])
interface ApplicationGraph {
    val httpClient: HttpClient
    val photoDownloadTracker: PhotoDownloadTracker
    val apiClient: UnsplashApiClient
    val repository: UnsplashRepository

    val photoDetailsViewModelFactory: PhotoDetailsViewModel.Factory
    val feedViewModel: FeedViewModel
    val randomPhotoViewModel: RandomPhotoViewModel
    val unifiedSearchViewModel: UnifiedSearchViewModel
    val collectionsFeedViewModel: CollectionsFeedViewModel
    val collectionDetailViewModelFactory: CollectionDetailViewModel.Factory
    val userProfileViewModelFactory: UserProfileViewModel.Factory
}

object MetroHelper {
    val graph: ApplicationGraph by lazy {
        createGraph<ApplicationGraph>()
    }

    fun getPhotoDetailsViewModel(photoId: String): PhotoDetailsViewModel = graph.photoDetailsViewModelFactory.create(photoId)

    fun getFeedViewModel(): FeedViewModel = graph.feedViewModel

    fun getRandomPhotoViewModel(): RandomPhotoViewModel = graph.randomPhotoViewModel

    fun getUnifiedSearchViewModel(): UnifiedSearchViewModel = graph.unifiedSearchViewModel

    fun getCollectionsFeedViewModel(): CollectionsFeedViewModel = graph.collectionsFeedViewModel

    fun getCollectionDetailViewModel(collectionId: String): CollectionDetailViewModel =
        graph.collectionDetailViewModelFactory.create(collectionId)

    fun getUserProfileViewModel(username: String): UserProfileViewModel = graph.userProfileViewModelFactory.create(username)
}
