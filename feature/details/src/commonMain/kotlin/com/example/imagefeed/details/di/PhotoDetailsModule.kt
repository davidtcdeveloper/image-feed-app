package com.example.imagefeed.details.di

import com.example.imagefeed.details.data.PhotoDetailsApiClient
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.details.data.PhotoDetailsRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient

@BindingContainer
object PhotoDetailsModule {
    @Provides
    fun providePhotoDetailsRepository(client: HttpClient): PhotoDetailsRepository =
        PhotoDetailsRepositoryImpl(PhotoDetailsApiClient(client))
}
