package com.example.imagefeed.search.di

import com.example.imagefeed.search.data.SearchApiClient
import com.example.imagefeed.search.data.SearchRepository
import com.example.imagefeed.search.data.SearchRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient

@BindingContainer
object SearchModule {
    @Provides
    fun provideSearchRepository(client: HttpClient): SearchRepository = SearchRepositoryImpl(SearchApiClient(client))
}
