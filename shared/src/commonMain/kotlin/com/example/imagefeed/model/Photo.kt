package com.example.imagefeed.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    val width: Int,
    val height: Int,
    val color: String? = null,
    @SerialName("blur_hash") val blurHash: String? = null,
    val description: String? = null,
    @SerialName("alt_description") val altDescription: String? = null,
    val urls: PhotoUrls,
    val user: User,
    val links: PhotoLinks,
    val exif: Exif? = null,
    val location: Location? = null,
    val tags: List<Tag>? = null
) {
    // Helper to calculate height given a target width (maintains original aspect ratio)
    fun getAspectRatioHeight(targetWidth: Int): Int {
        if (width <= 0 || height <= 0) return targetWidth
        return (targetWidth.toDouble() * (height.toDouble() / width.toDouble())).toInt()
    }
}

@Serializable
data class PhotoUrls(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String
)

@Serializable
data class User(
    val id: String,
    val username: String,
    val name: String,
    @SerialName("portfolio_url") val portfolioUrl: String? = null,
    @SerialName("profile_image") val profileImage: ProfileImage,
    val links: UserLinks,
    val bio: String? = null,
    val location: String? = null,
    @SerialName("total_likes") val totalLikes: Int? = null,
    @SerialName("total_photos") val totalPhotos: Int? = null,
    @SerialName("total_collections") val totalCollections: Int? = null,
    val social: Social? = null
)

@Serializable
data class Social(
    @SerialName("instagram_username") val instagramUsername: String? = null,
    @SerialName("twitter_username") val twitterUsername: String? = null,
    @SerialName("portfolio_url") val portfolioUrl: String? = null
)

@Serializable
data class UserStats(
    val username: String,
    val downloads: StatMetric,
    val views: StatMetric
)

@Serializable
data class ProfileImage(
    val small: String,
    val medium: String,
    val large: String
)

@Serializable
data class UserLinks(
    val self: String,
    val html: String,
    val photos: String
)

@Serializable
data class PhotoLinks(
    val self: String,
    val html: String,
    val download: String,
    @SerialName("download_location") val downloadLocation: String
)

@Serializable
data class Exif(
    val make: String? = null,
    val model: String? = null,
    @SerialName("exposure_time") val exposureTime: String? = null,
    val aperture: String? = null,
    @SerialName("focal_length") val focalLength: String? = null,
    val iso: Int? = null
)

@Serializable
data class Location(
    val name: String? = null,
    val city: String? = null,
    val country: String? = null,
    val position: Position? = null
)

@Serializable
data class Position(
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class Tag(
    val title: String
)

@Serializable
data class StatsValue(
    val date: String,
    val value: Int
)

@Serializable
data class HistoricalStats(
    val change: Int? = null,
    val average: Int? = null,
    val resolution: String? = null,
    val quantity: Int? = null,
    val values: List<StatsValue> = emptyList()
)

@Serializable
data class StatMetric(
    val total: Int,
    val historical: HistoricalStats? = null
)

@Serializable
data class PhotoStats(
    val id: String,
    val downloads: StatMetric,
    val views: StatMetric,
    val likes: StatMetric? = null
)
