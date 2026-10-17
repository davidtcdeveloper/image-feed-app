package com.example.imagefeed.presentation.profile

import com.example.imagefeed.model.AppError
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.mvi.UiState
import com.example.imagefeed.profile.model.UserStats

enum class ProfileTab { PORTFOLIO, LIKES, COLLECTIONS, INSIGHTS }

data class UserProfileState(
    val user: User? = null,
    val activeTab: ProfileTab = ProfileTab.PORTFOLIO,
    val portfolioPhotos: List<Photo> = emptyList(),
    val likedPhotos: List<Photo> = emptyList(),
    val collections: List<PhotoCollection> = emptyList(),
    val stats: UserStats? = null,
    val isLoadingContent: Boolean = false,
    val isHeaderLoading: Boolean = false,
    val error: AppError? = null,
    val portfolioPage: Int = 1,
    val portfolioReachedEnd: Boolean = false,
    val likesPage: Int = 1,
    val likesReachedEnd: Boolean = false,
    val collectionsPage: Int = 1,
    val collectionsReachedEnd: Boolean = false,
    val isLoadingStats: Boolean = false,
) : UiState
