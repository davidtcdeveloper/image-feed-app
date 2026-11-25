package com.example.imagefeed.di

import com.example.imagefeed.collections.data.CollectionsRepository
import com.example.imagefeed.collections.di.CollectionsModule
import com.example.imagefeed.core.database.FavoritesRepository
import com.example.imagefeed.core.database.di.DatabaseModule
import com.example.imagefeed.core.network.KtorPhotoDownloadTracker
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.core.network.createUnsplashHttpClient
import com.example.imagefeed.core.network.di.AppScope
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.details.di.PhotoDetailsModule
import com.example.imagefeed.feed.data.FeedRepository
import com.example.imagefeed.feed.di.FeedModule
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.model.cache.NavigationPreviewCache
import com.example.imagefeed.presentation.collection.CollectionDetailViewModel
import com.example.imagefeed.presentation.collections.CollectionsFeedViewModel
import com.example.imagefeed.presentation.details.PhotoDetailsViewModel
import com.example.imagefeed.presentation.favorites.FavoritesViewModel
import com.example.imagefeed.presentation.feed.FeedViewModel
import com.example.imagefeed.presentation.profile.UserProfileViewModel
import com.example.imagefeed.presentation.random.RandomPhotoViewModel
import com.example.imagefeed.presentation.search.UnifiedSearchViewModel
import com.example.imagefeed.profile.data.ProfileRepository
import com.example.imagefeed.profile.di.ProfileModule
import com.example.imagefeed.search.data.SearchRepository
import com.example.imagefeed.search.di.SearchModule
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.ktor.client.HttpClient

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
@BindingContainer
internal object AppModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(): HttpClient = createUnsplashHttpClient()

    @Provides
    @SingleIn(AppScope::class)
    fun providePhotoDownloadTracker(client: HttpClient): PhotoDownloadTracker = KtorPhotoDownloadTracker(client)

    @Provides
    @SingleIn(AppScope::class)
    fun provideNavigationPreviewCache(): NavigationPreviewCache = NavigationPreviewCache()
}

@SingleIn(AppScope::class)
@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [
        AppModule::class,
        DatabaseModule::class,
        PhotoDetailsModule::class,
        FeedModule::class,
        SearchModule::class,
        CollectionsModule::class,
        ProfileModule::class,
    ],
)
internal interface ApplicationGraph {
    val favoritesRepository: FavoritesRepository
    val photoDetailsRepository: PhotoDetailsRepository
    val feedRepository: FeedRepository
    val searchRepository: SearchRepository
    val collectionsRepository: CollectionsRepository
    val profileRepository: ProfileRepository

    val favoritesViewModel: FavoritesViewModel
    val photoDetailsViewModelFactory: PhotoDetailsViewModel.Factory
    val feedViewModel: FeedViewModel
    val randomPhotoViewModelFactory: RandomPhotoViewModel.Factory
    val unifiedSearchViewModel: UnifiedSearchViewModel
    val collectionsFeedViewModel: CollectionsFeedViewModel
    val collectionDetailViewModelFactory: CollectionDetailViewModel.Factory
    val userProfileViewModelFactory: UserProfileViewModel.Factory
    val navigationPreviewCache: NavigationPreviewCache
}

@Suppress("TooManyFunctions", "MaxLineLength")
object MetroHelper {
    internal val graph: ApplicationGraph by lazy {
        createGraph<ApplicationGraph>()
    }

    fun getFavoritesViewModel(): FavoritesViewModel = graph.favoritesViewModel

    fun getPhotoDetailsViewModel(photoId: String): PhotoDetailsViewModel = graph.photoDetailsViewModelFactory.create(photoId)

    fun getFeedViewModel(): FeedViewModel = graph.feedViewModel

    fun getRandomPhotoViewModel(): RandomPhotoViewModel = getRandomPhotoViewModel(autoFetchOnInit = true)

    fun getRandomPhotoViewModel(autoFetchOnInit: Boolean): RandomPhotoViewModel = graph.randomPhotoViewModelFactory.create(autoFetchOnInit)

    fun getUnifiedSearchViewModel(): UnifiedSearchViewModel = graph.unifiedSearchViewModel

    fun getCollectionsFeedViewModel(): CollectionsFeedViewModel = graph.collectionsFeedViewModel

    fun getCollectionDetailViewModel(collectionId: String): CollectionDetailViewModel =
        graph.collectionDetailViewModelFactory.create(collectionId)

    fun getUserProfileViewModel(username: String): UserProfileViewModel = graph.userProfileViewModelFactory.create(username)

    @Suppress("unused") // Invoked on Android & Swift navigation entry points
    fun seedPhotoPreview(photo: Photo) = graph.navigationPreviewCache.seedPhoto(photo)

    @Suppress("unused") // Invoked on Android & Swift navigation entry points
    fun getPhotoPreview(photoId: String): Photo? = graph.navigationPreviewCache.getPhoto(photoId)

    @Suppress("unused") // Invoked on Android & Swift navigation entry points
    fun seedCollectionPreview(collection: PhotoCollection) = graph.navigationPreviewCache.seedCollection(collection)

    @Suppress("unused") // Invoked on Android & Swift navigation entry points
    fun getCollectionPreview(collectionId: String): PhotoCollection? = graph.navigationPreviewCache.getCollection(collectionId)

    @Suppress("unused") // Invoked on Android & Swift navigation entry points
    fun seedUserPreview(user: User) = graph.navigationPreviewCache.seedUser(user)

    @Suppress("unused") // Invoked on Android & Swift navigation entry points
    fun getUserPreview(username: String): User? = graph.navigationPreviewCache.getUser(username)

    @Deprecated(
        message =
            "Use MetroHelper.getRandomPhotoViewModel(autoFetchOnInit = false) instead; " +
                "pending deletion in Spec 84.",
    )
    @OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
    @kotlin.native.HiddenFromObjC
    fun getFeedRepository(): FeedRepository = graph.feedRepository
}
