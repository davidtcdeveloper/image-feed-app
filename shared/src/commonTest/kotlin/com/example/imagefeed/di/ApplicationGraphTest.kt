package com.example.imagefeed.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class ApplicationGraphTest {
    @Test
    fun graphInstantiatesAndResolvesAllFeatureViewModelsAndFactories() {
        val graph = MetroHelper.graph
        assertNotNull(graph, "ApplicationGraph should not be null")
        assertNotNull(graph.photoDetailsRepository, "PhotoDetailsRepository should not be null")
        assertNotNull(graph.feedRepository, "FeedRepository should not be null")
        assertNotNull(graph.searchRepository, "SearchRepository should not be null")
        assertNotNull(graph.collectionsRepository, "CollectionsRepository should not be null")
        assertNotNull(graph.profileRepository, "ProfileRepository should not be null")
        val feedVm = MetroHelper.getFeedViewModel()
        assertNotNull(feedVm, "FeedViewModel should be resolvable")
        feedVm.clear()

        val feedRepo = MetroHelper.getFeedRepository()
        assertNotNull(feedRepo, "FeedRepository should be resolvable via MetroHelper")

        val randomVm = MetroHelper.getRandomPhotoViewModel()
        assertNotNull(randomVm, "RandomPhotoViewModel should be resolvable")
        randomVm.clear()

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
    }
}
