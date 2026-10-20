package com.example.imagefeed.search.di

import com.example.imagefeed.core.network.di.AppScope
import com.example.imagefeed.search.data.SearchApiClient
import com.example.imagefeed.search.data.SearchRepository
import com.example.imagefeed.search.data.SearchRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
@BindingContainer
object SearchModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideSearchRepository(client: HttpClient): SearchRepository = SearchRepositoryImpl(SearchApiClient(client))
}
