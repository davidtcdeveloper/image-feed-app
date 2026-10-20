package com.example.imagefeed.profile.di

import com.example.imagefeed.core.network.di.AppScope
import com.example.imagefeed.profile.data.ProfileApiClient
import com.example.imagefeed.profile.data.ProfileRepository
import com.example.imagefeed.profile.data.ProfileRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
@BindingContainer
object ProfileModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideProfileRepository(client: HttpClient): ProfileRepository = ProfileRepositoryImpl(ProfileApiClient(client))
}
