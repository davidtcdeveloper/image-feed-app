package com.example.imagefeed.android

import android.app.Application
import com.example.imagefeed.di.initKoin
import org.koin.android.ext.koin.androidContext

class ImageFeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initKoin {
            androidContext(this@ImageFeedApplication)
        }
    }
}
