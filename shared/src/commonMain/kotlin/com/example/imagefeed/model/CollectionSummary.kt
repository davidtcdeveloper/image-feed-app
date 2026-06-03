package com.example.imagefeed.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionSummary(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("total_photos") val totalPhotos: Int,
    @SerialName("cover_photo") val coverPhoto: Photo? = null,
    val user: User,
    val links: CollectionLinks? = null
)

@Serializable
data class CollectionLinks(
    val self: String,
    val html: String,
    val photos: String,
    val related: String? = null
)

@Serializable
data class SearchResponse<T>(
    val total: Int,
    @SerialName("total_pages") val totalPages: Int,
    val results: List<T>
)
