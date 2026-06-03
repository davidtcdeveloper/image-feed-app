package com.example.imagefeed.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotoCollection(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("total_photos") val totalPhotos: Int,
    val private: Boolean? = null,
    @SerialName("cover_photo") val coverPhoto: Photo? = null,
    val user: User,
    val links: CollectionLinks? = null,
    @SerialName("preview_photos") val previewPhotos: List<PreviewPhoto>? = null
)

@Serializable
data class PreviewPhoto(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val urls: PhotoUrls
)
