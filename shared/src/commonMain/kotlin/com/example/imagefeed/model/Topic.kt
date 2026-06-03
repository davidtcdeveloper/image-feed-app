package com.example.imagefeed.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("total_photos") val totalPhotos: Int,
    val status: String? = null,
    @SerialName("cover_photo") val coverPhoto: Photo? = null,
    val owners: List<User>? = null
)
