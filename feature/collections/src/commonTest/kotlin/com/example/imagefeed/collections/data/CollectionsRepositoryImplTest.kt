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

class CollectionsRepositoryImplTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val sampleCollection =
        """
        {
            "id": "col-r1",
            "title": "Minimal",
            "total_photos": 5,
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
                        content = "[$sampleCollection]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@CollectionsRepositoryImplTest.json) } }
            val apiClient = CollectionsApiClient(client)
            val repository = CollectionsRepositoryImpl(apiClient)

            val result = repository.getCollections(1, 10)
            assertEquals(1, result.size)
            assertEquals("col-r1", result[0].id)
        }
}
