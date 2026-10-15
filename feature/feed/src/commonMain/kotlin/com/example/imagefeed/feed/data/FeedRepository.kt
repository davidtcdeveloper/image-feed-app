package com.example.imagefeed.feed.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.Topic

interface FeedRepository {
    suspend fun getPhotos(
        page: Int,
        perPage: Int,
    ): List<Photo>

    suspend fun getRandomPhotos(
        orientation: String? = null,
        query: String? = null,
        count: Int? = null,
    ): List<Photo>

    suspend fun getTopics(
        page: Int,
        perPage: Int,
    ): List<Topic>

    suspend fun getTopicPhotos(
        idOrSlug: String,
        page: Int,
        perPage: Int,
    ): List<Photo>
}
