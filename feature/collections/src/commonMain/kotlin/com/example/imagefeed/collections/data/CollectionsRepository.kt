package com.example.imagefeed.collections.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection

interface CollectionsRepository {
    suspend fun getCollections(
        page: Int,
        perPage: Int,
    ): List<PhotoCollection>

    suspend fun getCollection(id: String): PhotoCollection

    suspend fun getCollectionPhotos(
        id: String,
        page: Int,
        perPage: Int,
    ): List<Photo>

    suspend fun getRelatedCollections(id: String): List<PhotoCollection>
}
