package com.example.imagefeed.api

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
import io.ktor.client.request.get
import io.ktor.client.request.parameter

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
}
