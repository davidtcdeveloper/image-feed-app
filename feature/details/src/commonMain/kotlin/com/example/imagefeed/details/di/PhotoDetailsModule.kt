package com.example.imagefeed.details.di

import com.example.imagefeed.details.data.PhotoDetailsApiClient
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.details.data.PhotoDetailsRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
@BindingContainer
object PhotoDetailsModule {
    @Provides
    fun providePhotoDetailsRepository(client: HttpClient): PhotoDetailsRepository =
        PhotoDetailsRepositoryImpl(PhotoDetailsApiClient(client))
}
