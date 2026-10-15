package com.example.imagefeed.search.data

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

class SearchApiClientTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun searchPhotosQueriesCorrectEndpointAndParameters() =
        runTest {
            val responseJson =
                """
                {
                    "total": 1,
                    "total_pages": 1,
                    "results": [
                        {
                            "id": "p-search-1",
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
                    ]
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals("/search/photos", request.url.encodedPath)
                    assertEquals("nature", request.url.parameters["query"])
                    assertEquals("1", request.url.parameters["page"])
                    assertEquals("15", request.url.parameters["per_page"])
                    assertEquals("latest", request.url.parameters["order_by"])
                    assertEquals("black_and_white", request.url.parameters["color"])
                    assertEquals("landscape", request.url.parameters["orientation"])
                    assertEquals("high", request.url.parameters["content_filter"])
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@SearchApiClientTest.json) } }
            val apiClient = SearchApiClient(client)

            val result =
                apiClient.searchPhotos(
                    query = "nature",
                    page = 1,
                    perPage = 15,
                    orderBy = "latest",
                    color = "black_and_white",
                    orientation = "landscape",
                    contentFilter = "high",
                )

            assertEquals(1, result.total)
            assertEquals("p-search-1", result.results[0].id)
        }

    @Test
    fun searchCollectionsQueriesCorrectEndpoint() =
        runTest {
            val responseJson =
                """
                {
                    "total": 1,
                    "total_pages": 1,
                    "results": [
                        {
                            "id": "col-s1",
                            "title": "Wallpapers",
                            "total_photos": 10,
                            "user": {
                                "id": "u", "username": "usr", "name": "Name",
                                "profile_image": { "small": "s", "medium": "m", "large": "l" },
                                "links": { "self": "s", "html": "h", "photos": "p" }
                            }
                        }
                    ]
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals("/search/collections", request.url.encodedPath)
                    assertEquals("wallpapers", request.url.parameters["query"])
                    assertEquals("2", request.url.parameters["page"])
                    assertEquals("15", request.url.parameters["per_page"])
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@SearchApiClientTest.json) } }
            val apiClient = SearchApiClient(client)

            val result = apiClient.searchCollections(query = "wallpapers", page = 2, perPage = 15)
            assertEquals(1, result.total)
            assertEquals("col-s1", result.results[0].id)
        }

    @Test
    fun searchUsersQueriesCorrectEndpoint() =
        runTest {
            val responseJson =
                """
                {
                    "total": 1,
                    "total_pages": 1,
                    "results": [
                        {
                            "id": "u-s1",
                            "username": "artist",
                            "name": "Artist Name",
                            "profile_image": { "small": "s", "medium": "m", "large": "l" },
                            "links": { "self": "s", "html": "h", "photos": "p" }
                        }
                    ]
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    assertEquals("/search/users", request.url.encodedPath)
                    assertEquals("artist", request.url.parameters["query"])
                    assertEquals("1", request.url.parameters["page"])
                    assertEquals("15", request.url.parameters["per_page"])
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@SearchApiClientTest.json) } }
            val apiClient = SearchApiClient(client)

            val result = apiClient.searchUsers(query = "artist", page = 1, perPage = 15)
            assertEquals(1, result.total)
            assertEquals("artist", result.results[0].username)
        }
}
