package com.example.imagefeed.repository

import com.example.imagefeed.api.UnsplashApiClient
import com.example.imagefeed.core.network.PhotoDownloadTracker
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsplashRepositoryImplTest {
    private class FakePhotoDownloadTracker : PhotoDownloadTracker {
        val calls = mutableListOf<String>()

        override suspend fun trackDownload(downloadLocationUrl: String) {
            calls.add(downloadLocationUrl)
        }
    }

    @Test
    fun trackDownloadDelegatesDirectlyToPhotoDownloadTracker() =
        runTest {
            val fakeTracker = FakePhotoDownloadTracker()
            val dummyClient = HttpClient(MockEngine { respondOk() })
            val apiClient = UnsplashApiClient(dummyClient)
            val repository = UnsplashRepositoryImpl(apiClient, fakeTracker)

            val testLocation = "https://api.unsplash.com/photos/abc/download?ixid=xyz"
            repository.trackDownload(testLocation)

            assertEquals(1, fakeTracker.calls.size)
            assertEquals(testLocation, fakeTracker.calls[0])
        }
}
