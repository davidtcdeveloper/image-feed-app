package com.example.imagefeed.profile.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.profile.model.UserStats

internal class ProfileRepositoryImpl(
    private val apiClient: ProfileApiClient,
) : ProfileRepository {
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
}
