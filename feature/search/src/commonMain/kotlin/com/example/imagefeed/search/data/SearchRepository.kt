package com.example.imagefeed.search.data

import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.User

interface SearchRepository {
    suspend fun searchPhotos(
        query: String,
        page: Int,
        perPage: Int,
        orderBy: String? = null,
        color: String? = null,
        orientation: String? = null,
        contentFilter: String? = null,
    ): SearchResponse<Photo>

    suspend fun searchCollections(
        query: String,
        page: Int,
        perPage: Int,
    ): SearchResponse<CollectionSummary>

    suspend fun searchUsers(
        query: String,
        page: Int,
        perPage: Int,
    ): SearchResponse<User>
}
