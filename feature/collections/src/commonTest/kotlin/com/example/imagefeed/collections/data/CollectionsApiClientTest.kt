package com.example.imagefeed.collections.data

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

class CollectionsApiClientTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val sampleCollection =
        """
        {
            "id": "col-123",
            "title": "Minimal",
            "total_photos": 5,
            "user": {
                "id": "u", "username": "usr", "name": "Name",
                "profile_image": { "small": "s", "medium": "m", "large": "l" },
                "links": { "self": "s", "html": "h", "photos": "p" }
            }
        }
        """.trimIndent()

    private val samplePhoto =
        """
        {
            "id": "p-col-1",
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
    fun getCollectionsQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/collections", request.url.encodedPath)
                    assertEquals("1", request.url.parameters["page"])
                    assertEquals("10", request.url.parameters["per_page"])
                    respond(
                        content = "[$sampleCollection]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@CollectionsApiClientTest.json) } }
            val apiClient = CollectionsApiClient(client)

            val result = apiClient.getCollections(1, 10)
            assertEquals(1, result.size)
            assertEquals("col-123", result[0].id)
        }

    @Test
    fun getCollectionQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/collections/col-123", request.url.encodedPath)
                    respond(
                        content = sampleCollection,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@CollectionsApiClientTest.json) } }
            val apiClient = CollectionsApiClient(client)

            val result = apiClient.getCollection("col-123")
            assertEquals("col-123", result.id)
            assertEquals("Minimal", result.title)
        }

    @Test
    fun getCollectionPhotosQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/collections/col-123/photos", request.url.encodedPath)
                    assertEquals("2", request.url.parameters["page"])
                    assertEquals("15", request.url.parameters["per_page"])
                    respond(
                        content = "[$samplePhoto]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@CollectionsApiClientTest.json) } }
            val apiClient = CollectionsApiClient(client)

            val result = apiClient.getCollectionPhotos("col-123", 2, 15)
            assertEquals(1, result.size)
            assertEquals("p-col-1", result[0].id)
        }

    @Test
    fun getRelatedCollectionsQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/collections/col-123/related", request.url.encodedPath)
                    respond(
                        content = "[$sampleCollection]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@CollectionsApiClientTest.json) } }
            val apiClient = CollectionsApiClient(client)

            val result = apiClient.getRelatedCollections("col-123")
            assertEquals(1, result.size)
            assertEquals("col-123", result[0].id)
        }
}
