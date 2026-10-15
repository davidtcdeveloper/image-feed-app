package com.example.imagefeed.details.data

import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.Photo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class PhotoDetailsApiClient(
    private val client: HttpClient,
) {
    suspend fun getPhotoDetails(id: String): Photo = client.get("photos/$id").body()

    suspend fun getPhotoStats(id: String): PhotoStats = client.get("photos/$id/statistics").body()
}
