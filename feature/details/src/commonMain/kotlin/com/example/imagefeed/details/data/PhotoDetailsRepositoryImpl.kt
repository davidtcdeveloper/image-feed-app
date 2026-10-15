package com.example.imagefeed.details.data

import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.Photo

internal class PhotoDetailsRepositoryImpl(
    private val apiClient: PhotoDetailsApiClient,
) : PhotoDetailsRepository {
    override suspend fun getPhotoDetails(id: String): Photo = apiClient.getPhotoDetails(id)

    override suspend fun getPhotoStats(id: String): PhotoStats = apiClient.getPhotoStats(id)
}
