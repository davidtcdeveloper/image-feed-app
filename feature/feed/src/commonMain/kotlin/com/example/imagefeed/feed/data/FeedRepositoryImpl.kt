package com.example.imagefeed.feed.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.Topic

internal class FeedRepositoryImpl(
    private val apiClient: FeedApiClient,
) : FeedRepository {
    override suspend fun getPhotos(
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getPhotos(page, perPage)

    override suspend fun getRandomPhotos(
        orientation: String?,
        query: String?,
        count: Int?,
    ): List<Photo> = apiClient.getRandomPhotos(orientation, query, count)

    override suspend fun getTopics(
        page: Int,
        perPage: Int,
    ): List<Topic> = apiClient.getTopics(page, perPage)

    override suspend fun getTopicPhotos(
        idOrSlug: String,
        page: Int,
        perPage: Int,
    ): List<Photo> = apiClient.getTopicPhotos(idOrSlug, page, perPage)
}
