package com.example.imagefeed.di

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class ApplicationGraphTest {
    @Test
    fun graphInstantiatesAndResolvesAllFeatureViewModelsAndFactories() {
        val graph = MetroHelper.graph
        assertNotNull(graph, "ApplicationGraph should not be null")
        assertNotNull(graph.favoritesRepository, "FavoritesRepository should not be null")
        assertNotNull(graph.photoDetailsRepository, "PhotoDetailsRepository should not be null")
        assertNotNull(graph.feedRepository, "FeedRepository should not be null")
        assertNotNull(graph.searchRepository, "SearchRepository should not be null")
        assertNotNull(graph.collectionsRepository, "CollectionsRepository should not be null")
        assertNotNull(graph.profileRepository, "ProfileRepository should not be null")
        assertNotNull(graph.navigationPreviewCache, "NavigationPreviewCache should not be null")

        val favoritesVm = MetroHelper.getFavoritesViewModel()
        assertNotNull(favoritesVm, "FavoritesViewModel should be resolvable")
        favoritesVm.clear()

        val feedVm = MetroHelper.getFeedViewModel()
        assertNotNull(feedVm, "FeedViewModel should be resolvable")
        feedVm.clear()

        @Suppress("DEPRECATION")
        val feedRepo = MetroHelper.getFeedRepository()
        assertNotNull(feedRepo, "FeedRepository should be resolvable via MetroHelper")

        val randomVm = MetroHelper.getRandomPhotoViewModel()
        assertNotNull(randomVm, "RandomPhotoViewModel should be resolvable")
        randomVm.clear()

        val idleRandomVm1 = MetroHelper.getRandomPhotoViewModel(autoFetchOnInit = false)
        val idleRandomVm2 = MetroHelper.getRandomPhotoViewModel(autoFetchOnInit = false)
        assertNotNull(idleRandomVm1)
        assertNotNull(idleRandomVm2)
        assertNotSame(idleRandomVm1, idleRandomVm2, "Factory must create distinct RandomPhotoViewModel instances")
        idleRandomVm1.clear()
        idleRandomVm2.clear()

        val searchVm = MetroHelper.getUnifiedSearchViewModel()
        assertNotNull(searchVm, "UnifiedSearchViewModel should be resolvable")
        searchVm.clear()

        val collectionsFeedVm = MetroHelper.getCollectionsFeedViewModel()
        assertNotNull(collectionsFeedVm, "CollectionsFeedViewModel should be resolvable")
        collectionsFeedVm.clear()

        // Verify assisted factories produce fresh, distinct instances for different IDs
        val colVm1 = MetroHelper.getCollectionDetailViewModel("col-1")
        val colVm2 = MetroHelper.getCollectionDetailViewModel("col-2")
        assertNotNull(colVm1)
        assertNotNull(colVm2)
        assertNotSame(colVm1, colVm2, "Factory must create distinct CollectionDetailViewModel instances")
        colVm1.clear()
        colVm2.clear()

        val userVm1 = MetroHelper.getUserProfileViewModel("user-1")
        val userVm2 = MetroHelper.getUserProfileViewModel("user-2")
        assertNotNull(userVm1)
        assertNotNull(userVm2)
        assertNotSame(userVm1, userVm2, "Factory must create distinct UserProfileViewModel instances")
        userVm1.clear()
        userVm2.clear()

        val photoVm1 = MetroHelper.getPhotoDetailsViewModel("photo-1")
        val photoVm2 = MetroHelper.getPhotoDetailsViewModel("photo-2")
        assertNotNull(photoVm1)
        assertNotNull(photoVm2)
        assertNotSame(photoVm1, photoVm2, "Factory must create distinct PhotoDetailsViewModel instances")
        photoVm1.clear()
        photoVm2.clear()

        // Verify MetroHelper preview cache seeding injects synchronously into PhotoDetailsViewModel Frame 0
        val previewUser =
            User(
                id = "u-preview",
                username = "previewer",
                name = "Preview User",
                profileImage = ProfileImage("s", "m", "l"),
                links = UserLinks("self", "html", "photos"),
            )
        val previewPhoto =
            Photo(
                id = "photo-preview-1",
                width = 200,
                height = 300,
                urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
                user = previewUser,
                links = PhotoLinks("self", "html", "download", "dl-loc"),
            )
        val previewCollection =
            PhotoCollection(
                id = "col-preview-1",
                title = "Preview Collection",
                totalPhotos = 5,
                user = previewUser,
            )

        MetroHelper.seedPhotoPreview(previewPhoto)
        MetroHelper.seedCollectionPreview(previewCollection)
        MetroHelper.seedUserPreview(previewUser)

        assertEquals(previewPhoto, MetroHelper.getPhotoPreview("photo-preview-1"))
        assertEquals(previewCollection, MetroHelper.getCollectionPreview("col-preview-1"))
        assertEquals(previewUser, MetroHelper.getUserPreview("previewer"))

        val seededDetailsVm = MetroHelper.getPhotoDetailsViewModel("photo-preview-1")
        assertEquals(previewPhoto, seededDetailsVm.state.value.photo)
        seededDetailsVm.clear()
        graph.navigationPreviewCache.clear()
    }
}
