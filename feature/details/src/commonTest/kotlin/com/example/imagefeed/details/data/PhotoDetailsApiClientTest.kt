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

class PhotoDetailsApiClientTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun getPhotoDetailsQueriesCorrectEndpointAndParsesResponse() =
        runTest {
            val responseJson =
                """
                {
                    "id": "photo-123",
                    "width": 1920,
                    "height": 1080,
                    "urls": {
                        "raw": "https://images.unsplash.com/raw",
                        "full": "https://images.unsplash.com/full",
                        "regular": "https://images.unsplash.com/regular",
                        "small": "https://images.unsplash.com/small",
                        "thumb": "https://images.unsplash.com/thumb"
                    },
                    "links": {
                        "self": "https://api.unsplash.com/photos/photo-123",
                        "html": "https://unsplash.com/photos/photo-123",
                        "download": "https://unsplash.com/photos/photo-123/download",
                        "download_location": "https://api.unsplash.com/photos/photo-123/download"
                    },
                    "user": {
                        "id": "user-456",
                        "username": "photographer",
                        "name": "Jane Doe",
                        "profile_image": {
                            "small": "https://images.unsplash.com/profile-small",
                            "medium": "https://images.unsplash.com/profile-medium",
                            "large": "https://images.unsplash.com/profile-large"
                        },
                        "links": {
                            "self": "https://api.unsplash.com/users/photographer",
                            "html": "https://unsplash.com/@photographer",
                            "photos": "https://api.unsplash.com/users/photographer/photos"
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals("/photos/photo-123", request.url.encodedPath)
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(this@PhotoDetailsApiClientTest.json)
                    }
                }

            val apiClient = PhotoDetailsApiClient(client)
            val photo = apiClient.getPhotoDetails("photo-123")

            assertEquals("photo-123", photo.id)
            assertEquals("Jane Doe", photo.user.name)
        }

    @Test
    fun getPhotoStatsQueriesCorrectEndpointAndParsesResponse() =
        runTest {
            val responseJson =
                """
                {
                    "id": "photo-123",
                    "downloads": {
                        "total": 1500,
                        "historical": {
                            "change": 25,
                            "average": 10,
                            "resolution": "days",
                            "quantity": 30,
                            "values": [
                                { "date": "2026-10-01", "value": 12 },
                                { "date": "2026-10-02", "value": 13 }
                            ]
                        }
                    },
                    "views": {
                        "total": 54000,
                        "historical": {
                            "change": 100,
                            "average": 50,
                            "resolution": "days",
                            "quantity": 30,
                            "values": []
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals("/photos/photo-123/statistics", request.url.encodedPath)
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(this@PhotoDetailsApiClientTest.json)
                    }
                }

            val apiClient = PhotoDetailsApiClient(client)
            val stats = apiClient.getPhotoStats("photo-123")

            assertEquals("photo-123", stats.id)
            assertEquals(1500, stats.downloads.total)
            assertEquals(54000, stats.views.total)
            assertEquals(
                2,
                stats.downloads.historical
                    ?.values
                    ?.size,
            )
        }

    @Test
    fun getPhotoStatsParsesLikesAndHandlesUnknownFields() =
        runTest {
            val responseJson =
                """
                {
                    "id": "photo-456",
                    "unexpected_meta": "ignored",
                    "downloads": {
                        "total": 500
                    },
                    "views": {
                        "total": 12000
                    },
                    "likes": {
                        "total": 350,
                        "historical": {
                            "change": 5,
                            "average": 2,
                            "resolution": "days",
                            "quantity": 7,
                            "values": [
                                { "date": "2026-10-10", "value": 3 }
                            ]
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals("/photos/photo-456/statistics", request.url.encodedPath)
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(this@PhotoDetailsApiClientTest.json)
                    }
                }

            val apiClient = PhotoDetailsApiClient(client)
            val stats = apiClient.getPhotoStats("photo-456")

            assertEquals("photo-456", stats.id)
            assertEquals(500, stats.downloads.total)
            assertEquals(12000, stats.views.total)
            assertEquals(350, stats.likes?.total)
            assertEquals(
                1,
                stats.likes
                    ?.historical
                    ?.values
                    ?.size,
            )
            assertEquals(
                "2026-10-10",
                stats.likes
                    ?.historical
                    ?.values
                    ?.first()
                    ?.date,
            )
        }
}
