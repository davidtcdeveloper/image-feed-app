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

class SearchRepositoryImplTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val sampleResponse =
        """
        {
            "total": 1,
            "total_pages": 1,
            "results": [
                {
                    "id": "p-repo-s",
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

    @Test
    fun repositoryDelegatesToApiClient() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = sampleResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@SearchRepositoryImplTest.json) } }
            val apiClient = SearchApiClient(client)
            val repository = SearchRepositoryImpl(apiClient)

            val result = repository.searchPhotos("test", 1, 15)
            assertEquals(1, result.total)
            assertEquals("p-repo-s", result.results[0].id)
        }
}
