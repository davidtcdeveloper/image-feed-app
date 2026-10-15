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

class ProfileRepositoryImplTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val sampleUser =
        """
        {
            "id": "u-prof-r",
            "username": "janedoe",
            "name": "Jane Doe",
            "profile_image": { "small": "s", "medium": "m", "large": "l" },
            "links": { "self": "s", "html": "h", "photos": "p" }
        }
        """.trimIndent()

    @Test
    fun repositoryDelegatesToApiClient() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = sampleUser,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(this@ProfileRepositoryImplTest.json) } }
            val apiClient = ProfileApiClient(client)
            val repository = ProfileRepositoryImpl(apiClient)

            val user = repository.getUserProfile("janedoe")
            assertEquals("janedoe", user.username)
        }
}
