package com.example.imagefeed.profile.data

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.profile.model.UserStats

interface ProfileRepository {
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
}
