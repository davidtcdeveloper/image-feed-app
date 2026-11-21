package com.example.imagefeed.di

import android.content.Context
import com.example.imagefeed.core.database.DatabaseContextHolder

object SharedAndroidInitializer {
    fun init(context: Context) {
        DatabaseContextHolder.init(context)
    }
}
