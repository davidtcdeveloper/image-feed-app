package com.example.imagefeed.collections.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection

internal class CollectionsRepositoryImpl(
    private val apiClient: CollectionsApiClient,
) : CollectionsRepository {
    override suspend fun getCollections(
        page: Int,
        perPage: Int,
    ): List<PhotoCollection> = apiClient.getCollections(page, perPage)

    override suspend fun getCollection(id: String): PhotoCollection = apiClient.getCollection(id)

    override suspend fun getCollectionPhotos(
        id: String,
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getCollectionPhotos(id, page, perPage)

    override suspend fun getRelatedCollections(id: String): List<PhotoCollection> = apiClient.getRelatedCollections(id)
}
