package com.example.imagefeed.feed.di

import com.example.imagefeed.feed.data.FeedApiClient
import com.example.imagefeed.feed.data.FeedRepository
import com.example.imagefeed.feed.data.FeedRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
@BindingContainer
object FeedModule {
    @Provides
    fun provideFeedRepository(client: HttpClient): FeedRepository = FeedRepositoryImpl(FeedApiClient(client))
}
