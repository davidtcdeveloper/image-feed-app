package com.example.imagefeed.feed.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedRepositoryImplTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val samplePhotoJson =
        """
        {
            "id": "p-repo",
            "width": 100,
            "height": 100,
            "urls": { "raw": "r", "full": "f", "regular": "reg", "small": "s", "thumb": "t" },
            "links": { "self": "s", "html": "h", "download": "d", "download_location": "dl" },
            "user": {
                "id": "u", "username": "usr", "name": "Name",
                "profile_image": { "small": "s", "medium": "m", "large": "l" },
                "links": { "self": "s", "html": "h", "photos": "p" }
            }
        }
        """.trimIndent()

    @Test
    fun repositoryDelegatesToApiClient() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = "[$samplePhotoJson]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@FeedRepositoryImplTest.json) } }
            val apiClient = FeedApiClient(client)
            val repository = FeedRepositoryImpl(apiClient)

            val photos = repository.getPhotos(1, 10)
            assertEquals(1, photos.size)
            assertEquals("p-repo", photos[0].id)
        }
}
