package com.example.imagefeed.di

import com.example.imagefeed.api.UnsplashApiClient
import com.example.imagefeed.api.createUnsplashHttpClient
import com.example.imagefeed.presentation.FeedPresenter
import com.example.imagefeed.presentation.PhotoDetailsPresenter
import com.example.imagefeed.presentation.RandomPhotoPresenter
import com.example.imagefeed.presentation.UnifiedSearchPresenter
import com.example.imagefeed.presentation.CollectionsFeedPresenter
import com.example.imagefeed.presentation.CollectionDetailPresenter
import com.example.imagefeed.presentation.UserProfilePresenter
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.repository.UnsplashRepositoryImpl
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { createUnsplashHttpClient() }
    single { UnsplashApiClient(get()) }
    single<UnsplashRepository> { UnsplashRepositoryImpl(get()) }
    factory { FeedPresenter(get()) }
    factory { (photoId: String) -> PhotoDetailsPresenter(get(), photoId) }
    factory { RandomPhotoPresenter(get()) }
    factory { UnifiedSearchPresenter(get()) }
    factory { CollectionsFeedPresenter(get()) }
    factory { (collectionId: String) -> CollectionDetailPresenter(get(), collectionId) }
    factory { (username: String) -> UserProfilePresenter(get(), username) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(commonModule)
}

// Helper for iOS DI initialization
fun initKoin() = initKoin {}

object KoinHelper : KoinComponent {
    fun getFeedPresenter(): FeedPresenter = getKoin().get()
    
    fun getPhotoDetailsPresenter(photoId: String): PhotoDetailsPresenter = 
        getKoin().get { parametersOf(photoId) }
        
    fun getRandomPhotoPresenter(): RandomPhotoPresenter = getKoin().get()
    
    fun getUnifiedSearchPresenter(): UnifiedSearchPresenter = getKoin().get()

    fun getCollectionsFeedPresenter(): CollectionsFeedPresenter = getKoin().get()

    fun getCollectionDetailPresenter(collectionId: String): CollectionDetailPresenter = 
        getKoin().get { parametersOf(collectionId) }

    fun getUserProfilePresenter(username: String): UserProfilePresenter = 
        getKoin().get { parametersOf(username) }
}
