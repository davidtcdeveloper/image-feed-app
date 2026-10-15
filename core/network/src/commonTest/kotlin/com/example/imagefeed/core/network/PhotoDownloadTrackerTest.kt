package com.example.imagefeed.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoDownloadTrackerTest {
    class FakePhotoDownloadTracker : PhotoDownloadTracker {
        val tracked = mutableListOf<String>()

        override suspend fun trackDownload(downloadLocationUrl: String) {
            tracked.add(downloadLocationUrl)
        }
    }

    @Test
    fun ktorTrackerExecutesGetRequestToDownloadLocation() =
        runTest {
            var capturedUrl: String? = null
            val mockEngine =
                MockEngine { request ->
                    capturedUrl = request.url.toString()
                    respond(
                        content = ByteReadChannel(""),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val tracker = KtorPhotoDownloadTracker(HttpClient(mockEngine))
            val testUrl = "https://api.unsplash.com/photos/1/download?ixid=test123"

            tracker.trackDownload(testUrl)

            assertEquals(testUrl, capturedUrl)
            assertEquals(1, mockEngine.requestHistory.size)
        }

    @Test
    fun ktorTrackerGracefullySwallowsNetworkFailure() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respondError(HttpStatusCode.InternalServerError, "Server error")
                }
            val tracker = KtorPhotoDownloadTracker(HttpClient(mockEngine))

            // Must not throw or propagate exception to caller
            tracker.trackDownload("https://api.unsplash.com/photos/1/download?ixid=test123")

            assertEquals(1, mockEngine.requestHistory.size)
        }

    @Test
    fun ktorTrackerGracefullySwallowsEngineException() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    throw RuntimeException("Network unreachable")
                }
            val tracker = KtorPhotoDownloadTracker(HttpClient(mockEngine))

            // Must not throw or propagate exception to caller
            tracker.trackDownload("https://api.unsplash.com/photos/1/download?ixid=test123")
        }

    @Test
    fun ktorTrackerIgnoresBlankUrls() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(""),
                        status = HttpStatusCode.OK,
                    )
                }
            val tracker = KtorPhotoDownloadTracker(HttpClient(mockEngine))

            tracker.trackDownload("")
            tracker.trackDownload("   ")

            assertEquals(0, mockEngine.requestHistory.size)
        }

    @Test
    fun fakePhotoDownloadTrackerRecordsCalls() =
        runTest {
            val tracker = FakePhotoDownloadTracker()
            tracker.trackDownload("https://api.unsplash.com/photos/1/download?ixid=123")

            assertEquals(1, tracker.tracked.size)
            assertEquals("https://api.unsplash.com/photos/1/download?ixid=123", tracker.tracked[0])
        }
}
