package com.example.imagefeed.profile.data

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

class ProfileApiClientTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val sampleUser =
        """
        {
            "id": "u-prof-1",
            "username": "janedoe",
            "name": "Jane Doe",
            "profile_image": { "small": "s", "medium": "m", "large": "l" },
            "links": { "self": "s", "html": "h", "photos": "p" }
        }
        """.trimIndent()

    private val sampleStats =
        """
        {
            "username": "janedoe",
            "downloads": {
                "total": 500,
                "historical": {
                    "change": 10,
                    "average": 5,
                    "resolution": "days",
                    "quantity": 30,
                    "values": [
                        { "date": "2026-10-01", "value": 5 }
                    ]
                }
            },
            "views": {
                "total": 1000,
                "historical": {
                    "change": 20,
                    "average": 10,
                    "resolution": "days",
                    "quantity": 30,
                    "values": []
                }
            }
        }
        """.trimIndent()

    @Test
    fun getUserProfileQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/users/janedoe", request.url.encodedPath)
                    respond(
                        content = sampleUser,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@ProfileApiClientTest.json) } }
            val apiClient = ProfileApiClient(client)

            val user = apiClient.getUserProfile("janedoe")
            assertEquals("janedoe", user.username)
            assertEquals("Jane Doe", user.name)
        }

    @Test
    fun getUserStatsQueriesCorrectEndpoint() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/users/janedoe/statistics", request.url.encodedPath)
                    assertEquals("days", request.url.parameters["resolution"])
                    assertEquals("30", request.url.parameters["quantity"])
                    respond(
                        content = sampleStats,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@ProfileApiClientTest.json) } }
            val apiClient = ProfileApiClient(client)

            val stats = apiClient.getUserStats("janedoe")
            assertEquals("janedoe", stats.username)
            assertEquals(500, stats.downloads.total)
            assertEquals(1000, stats.views.total)
        }
}
