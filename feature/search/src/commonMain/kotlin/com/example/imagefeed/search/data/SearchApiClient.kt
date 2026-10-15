package com.example.imagefeed.search.data

import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class SearchApiClient(
    private val client: HttpClient,
) {
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
}
