package com.example.imagefeed.api

import com.example.imagefeed.BuildKonfig
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.Topic
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserStats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UnsplashApiClient(
    private val client: HttpClient,
) {
    suspend fun getPhotos(
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("photos") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getPhotoDetails(id: String): Photo = client.get("photos/$id").body()

    suspend fun getPhotoStats(id: String): PhotoStats = client.get("photos/$id/statistics").body()

    suspend fun getRandomPhotos(
        orientation: String? = null,
        query: String? = null,
        count: Int? = null,
    ): List<Photo> =
        client
            .get("photos/random") {
                orientation?.let { parameter("orientation", it) }
                query?.let { parameter("query", it) }
                count?.let { parameter("count", it) }
            }.body()

    suspend fun searchPhotos(
        query: String,
        page: Int,
        perPage: Int,
        orderBy: String? = null,
        color: String? = null,
        orientation: String? = null,
        contentFilter: String? = null,
    ): SearchResponse<Photo> =
        client
            .get("search/photos") {
                parameter("query", query)
                parameter("page", page)
                parameter("per_page", perPage)
                orderBy?.let { parameter("order_by", it) }
                color?.let { parameter("color", it) }
                orientation?.let { parameter("orientation", it) }
                contentFilter?.let { parameter("content_filter", it) }
            }.body()

    suspend fun searchCollections(
        query: String,
        page: Int,
        perPage: Int,
    ): SearchResponse<CollectionSummary> =
        client
            .get("search/collections") {
                parameter("query", query)
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun searchUsers(
        query: String,
        page: Int,
        perPage: Int,
    ): SearchResponse<User> =
        client
            .get("search/users") {
                parameter("query", query)
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getCollections(
        page: Int,
        perPage: Int,
    ): List<PhotoCollection> =
        client
            .get("collections") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getCollection(id: String): PhotoCollection = client.get("collections/$id").body()

    suspend fun getCollectionPhotos(
        id: String,
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("collections/$id/photos") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getRelatedCollections(id: String): List<PhotoCollection> = client.get("collections/$id/related").body()

    suspend fun getTopics(
        page: Int,
        perPage: Int,
    ): List<Topic> =
        client
            .get("topics") {
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("order_by", "position")
            }.body()

    suspend fun getTopic(idOrSlug: String): Topic = client.get("topics/$idOrSlug").body()

    suspend fun getTopicPhotos(
        idOrSlug: String,
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("topics/$idOrSlug/photos") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserProfile(username: String): User = client.get("users/$username").body()

    suspend fun getUserPhotos(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("users/$username/photos") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserLikes(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("users/$username/likes") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserCollections(
        username: String,
        page: Int,
        perPage: Int,
    ): List<PhotoCollection> =
        client
            .get("users/$username/collections") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserStats(username: String): UserStats =
        client
            .get("users/$username/statistics") {
                parameter("resolution", "days")
                parameter("quantity", 30)
            }.body()

    // Unsplash guidelines: trigger a request to download_location to increment stats
    suspend fun trackDownload(downloadLocationUrl: String) {
        // The download_location endpoint is returned directly from the API and contains query params.
        // We will call it directly using the HttpClient.
        client.get {
            url(downloadLocationUrl)
        }
    }
}

// Factory to create HttpClient configured for Unsplash API
fun createUnsplashHttpClient(): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode >= 300) {
                    val errorText = response.bodyAsText()
                    // Try to parse the error message if it's JSON error format
                    val errorMessage =
                        try {
                            val element = Json.parseToJsonElement(errorText)
                            val errors = element.jsonObject["errors"]?.jsonArray
                            errors?.map { it.jsonPrimitive.content }?.joinToString(", ")
                                ?: errorText
                        } catch (e: Exception) {
                            errorText
                        }
                    throw Exception("Unsplash API Error (Status $statusCode): $errorMessage")
                }
            }
        }
        defaultRequest {
            url("https://api.unsplash.com/")
            header("Accept-Version", "v1")
            header("Authorization", "Client-ID ${BuildKonfig.UNSPLASH_API_KEY}")
        }
    }
