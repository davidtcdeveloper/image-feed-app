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

class FeedApiClientTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val samplePhotoJson =
        """
        {
            "id": "feed-p1",
            "width": 800,
            "height": 600,
            "urls": {
                "raw": "raw", "full": "full", "regular": "regular", "small": "small", "thumb": "thumb"
            },
            "links": {
                "self": "self", "html": "html", "download": "download", "download_location": "dl"
            },
            "user": {
                "id": "u1", "username": "feeduser", "name": "Feed User",
                "profile_image": { "small": "s", "medium": "m", "large": "l" },
                "links": { "self": "s", "html": "h", "photos": "p" }
            }
        }
        """.trimIndent()

    private val sampleTopicJson =
        """
        {
            "id": "top-1",
            "slug": "nature",
            "title": "Nature",
            "total_photos": 42
        }
        """.trimIndent()

    @Test
    fun getPhotosQueriesCorrectEndpointAndParsesResponse() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/photos", request.url.encodedPath)
                    assertEquals("2", request.url.parameters["page"])
                    assertEquals("15", request.url.parameters["per_page"])
                    respond(
                        content = "[$samplePhotoJson]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@FeedApiClientTest.json) } }
            val apiClient = FeedApiClient(client)

            val photos = apiClient.getPhotos(page = 2, perPage = 15)
            assertEquals(1, photos.size)
            assertEquals("feed-p1", photos[0].id)
            assertEquals("Feed User", photos[0].user.name)
        }

    @Test
    fun getRandomPhotosQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/photos/random", request.url.encodedPath)
                    assertEquals("landscape", request.url.parameters["orientation"])
                    assertEquals("nature", request.url.parameters["query"])
                    assertEquals("1", request.url.parameters["count"])
                    respond(
                        content = "[$samplePhotoJson]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@FeedApiClientTest.json) } }
            val apiClient = FeedApiClient(client)

            val photos = apiClient.getRandomPhotos(orientation = "landscape", query = "nature", count = 1)
            assertEquals(1, photos.size)
            assertEquals("feed-p1", photos[0].id)
        }

    @Test
    fun getTopicsQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/topics", request.url.encodedPath)
                    assertEquals("1", request.url.parameters["page"])
                    assertEquals("25", request.url.parameters["per_page"])
                    assertEquals("position", request.url.parameters["order_by"])
                    respond(
                        content = "[$sampleTopicJson]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@FeedApiClientTest.json) } }
            val apiClient = FeedApiClient(client)

            val topics = apiClient.getTopics(page = 1, perPage = 25)
            assertEquals(1, topics.size)
            assertEquals("nature", topics[0].slug)
        }

    @Test
    fun getTopicPhotosQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/topics/nature/photos", request.url.encodedPath)
                    assertEquals("1", request.url.parameters["page"])
                    assertEquals("15", request.url.parameters["per_page"])
                    respond(
                        content = "[$samplePhotoJson]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@FeedApiClientTest.json) } }
            val apiClient = FeedApiClient(client)

            val photos = apiClient.getTopicPhotos("nature", 1, 15)
            assertEquals(1, photos.size)
            assertEquals("feed-p1", photos[0].id)
        }
}
