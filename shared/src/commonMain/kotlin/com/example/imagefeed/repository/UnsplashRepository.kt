package com.example.imagefeed.repository

import com.example.imagefeed.api.UnsplashApiClient
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.model.SearchResponse
import com.example.imagefeed.model.Topic
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserStats

interface UnsplashRepository {
    suspend fun getPhotos(
        page: Int,
        perPage: Int,
    ): List<Photo>

    suspend fun getPhotoDetails(id: String): Photo

    suspend fun getPhotoStats(id: String): PhotoStats

    suspend fun getRandomPhotos(
        orientation: String? = null,
        query: String? = null,
        count: Int? = null,
    ): List<Photo>

    suspend fun trackDownload(downloadLocationUrl: String)

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

    suspend fun getTopics(
        page: Int,
        perPage: Int,
    ): List<Topic>

    suspend fun getTopic(idOrSlug: String): Topic

    suspend fun getTopicPhotos(
        idOrSlug: String,
        page: Int,
        perPage: Int,
    ): List<Photo>

    suspend fun getUserProfile(username: String): User

    suspend fun getUserPhotos(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo>

    suspend fun getUserLikes(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo>

    suspend fun getUserCollections(
        username: String,
        page: Int,
        perPage: Int,
    ): List<PhotoCollection>

    suspend fun getUserStats(username: String): UserStats

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

class UnsplashRepositoryImpl(
    private val apiClient: UnsplashApiClient,
) : UnsplashRepository {
    override suspend fun getPhotos(
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getPhotos(page, perPage)

    override suspend fun getPhotoDetails(id: String): Photo = apiClient.getPhotoDetails(id)

    override suspend fun getPhotoStats(id: String): PhotoStats = apiClient.getPhotoStats(id)

    override suspend fun getRandomPhotos(
        orientation: String?,
        query: String?,
        count: Int?,
    ): List<Photo> = apiClient.getRandomPhotos(orientation, query, count)

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

    override suspend fun getTopics(
        page: Int,
        perPage: Int,
    ): List<Topic> = apiClient.getTopics(page, perPage)

    override suspend fun getTopic(idOrSlug: String): Topic = apiClient.getTopic(idOrSlug)

    override suspend fun getTopicPhotos(
        idOrSlug: String,
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getTopicPhotos(idOrSlug, page, perPage)

    override suspend fun getUserProfile(username: String): User = apiClient.getUserProfile(username)

    override suspend fun getUserPhotos(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getUserPhotos(username, page, perPage)

    override suspend fun getUserLikes(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getUserLikes(username, page, perPage)

    override suspend fun getUserCollections(
        username: String,
        page: Int,
        perPage: Int,
    ): List<PhotoCollection> = apiClient.getUserCollections(username, page, perPage)

    override suspend fun getUserStats(username: String): UserStats = apiClient.getUserStats(username)

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

    override suspend fun trackDownload(downloadLocationUrl: String) {
        // Run asynchronously to not block UI/user flow since it is a background tracking call
        try {
            apiClient.trackDownload(downloadLocationUrl)
        } catch (e: Exception) {
            // Log or ignore tracking failures so it doesn't crash the app
            println("Download tracking failed: ${e.message}")
        }
    }
}
