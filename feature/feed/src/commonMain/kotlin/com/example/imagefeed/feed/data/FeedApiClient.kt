package com.example.imagefeed.feed.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.Topic
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class FeedApiClient(
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
}
