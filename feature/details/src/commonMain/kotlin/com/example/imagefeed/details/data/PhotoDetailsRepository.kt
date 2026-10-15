package com.example.imagefeed.details.data

import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.Photo

interface PhotoDetailsRepository {
    suspend fun getPhotoDetails(id: String): Photo

    suspend fun getPhotoStats(id: String): PhotoStats
}
