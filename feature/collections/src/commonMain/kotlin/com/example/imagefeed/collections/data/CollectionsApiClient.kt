package com.example.imagefeed.collections.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class CollectionsApiClient(
    private val client: HttpClient,
) {
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
}
