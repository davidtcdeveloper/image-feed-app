package com.example.imagefeed.profile.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.profile.model.UserStats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class ProfileApiClient(
    private val client: HttpClient,
) {
    suspend fun getUserProfile(username: String): User = client.get("users/$username").body()

    suspend fun getUserPhotos(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("users/$username/photos") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserLikes(
        username: String,
        page: Int,
        perPage: Int,
    ): List<Photo> =
        client
            .get("users/$username/likes") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserCollections(
        username: String,
        page: Int,
        perPage: Int,
    ): List<PhotoCollection> =
        client
            .get("users/$username/collections") {
                parameter("page", page)
                parameter("per_page", perPage)
            }.body()

    suspend fun getUserStats(username: String): UserStats =
        client
            .get("users/$username/statistics") {
                parameter("resolution", "days")
                parameter("quantity", 30)
            }.body()
}
