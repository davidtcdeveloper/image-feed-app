package com.example.imagefeed.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url

interface PhotoDownloadTracker {
    suspend fun trackDownload(downloadLocationUrl: String)
}

class KtorPhotoDownloadTracker(
    private val client: HttpClient,
) : PhotoDownloadTracker {
    override suspend fun trackDownload(downloadLocationUrl: String) {
        if (downloadLocationUrl.isBlank()) return
        try {
            client.get {
                url(downloadLocationUrl)
            }
        } catch (e: Exception) {
            println("Download tracking failed: ${e.message}")
        }
    }
}
