package com.example.imagefeed.presentation.profile

import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface UserProfileIntent : UiIntent {
    data object LoadProfile : UserProfileIntent

    data class SelectTab(
        val tab: ProfileTab,
    ) : UserProfileIntent

    data object LoadNextPage : UserProfileIntent

    data object LoadNextPhotosPage : UserProfileIntent

    data object LoadNextLikesPage : UserProfileIntent

    data object LoadNextCollectionsPage : UserProfileIntent

    data object Refresh : UserProfileIntent

    data class TrackDownload(
        val photo: Photo,
    ) : UserProfileIntent
}
