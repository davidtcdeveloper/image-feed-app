package com.example.imagefeed.details.data

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

class PhotoDetailsRepositoryImplTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun repositoryDelegatesToApiClient() =
        runTest {
            val photoJson =
                """
                {
                    "id": "photo-test",
                    "width": 100,
                    "height": 100,
                    "urls": {
                        "raw": "raw", "full": "full", "regular": "regular", "small": "small", "thumb": "thumb"
                    },
                    "links": {
                        "self": "self", "html": "html", "download": "download", "download_location": "location"
                    },
                    "user": {
                        "id": "u1", "username": "u1", "name": "User 1",
                        "profile_image": { "small": "s", "medium": "m", "large": "l" },
                        "links": { "self": "s", "html": "h", "photos": "p" }
                    }
                }
                """.trimIndent()

            val statsJson =
                """
                {
                    "id": "photo-test",
                    "downloads": { "total": 99 },
                    "views": { "total": 999 }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/photos/photo-test" ->
                            respond(photoJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                        "/photos/photo-test/statistics" ->
                            respond(statsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                        else ->
                            respond("Not found", HttpStatusCode.NotFound)
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(this@PhotoDetailsRepositoryImplTest.json)
                    }
                }

            val apiClient = PhotoDetailsApiClient(client)
            val repository = PhotoDetailsRepositoryImpl(apiClient)

            val photo = repository.getPhotoDetails("photo-test")
            val stats = repository.getPhotoStats("photo-test")

            assertEquals("photo-test", photo.id)
            assertEquals(99, stats.downloads.total)
            assertEquals(999, stats.views.total)
        }
}
