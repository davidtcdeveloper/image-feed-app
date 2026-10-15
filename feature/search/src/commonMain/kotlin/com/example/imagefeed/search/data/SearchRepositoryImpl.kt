package com.example.imagefeed.search.data

import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.User

internal class SearchRepositoryImpl(
    private val apiClient: SearchApiClient,
) : SearchRepository {
    override suspend fun searchPhotos(
        query: String,
        page: Int,
        perPage: Int,
        orderBy: String?,
        color: String?,
        orientation: String?,
        contentFilter: String?,
    ): SearchResponse<Photo> = apiClient.searchPhotos(query, page, perPage, orderBy, color, orientation, contentFilter)

    override suspend fun searchCollections(
        query: String,
        page: Int,
        perPage: Int,
    ): SearchResponse<CollectionSummary> = apiClient.searchCollections(query, page, perPage)

    override suspend fun searchUsers(
        query: String,
        page: Int,
        perPage: Int,
    ): SearchResponse<User> = apiClient.searchUsers(query, page, perPage)
}
