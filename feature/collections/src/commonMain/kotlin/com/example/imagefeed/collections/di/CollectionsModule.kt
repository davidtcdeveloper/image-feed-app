package com.example.imagefeed.collections.di

import com.example.imagefeed.collections.data.CollectionsApiClient
import com.example.imagefeed.collections.data.CollectionsRepository
import com.example.imagefeed.collections.data.CollectionsRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient

@BindingContainer
object CollectionsModule {
    @Provides
    fun provideCollectionsRepository(client: HttpClient): CollectionsRepository = CollectionsRepositoryImpl(CollectionsApiClient(client))
}
