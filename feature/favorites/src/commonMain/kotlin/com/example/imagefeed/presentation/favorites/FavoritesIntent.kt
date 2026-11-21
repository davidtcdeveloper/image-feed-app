package com.example.imagefeed.presentation.favorites

import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface FavoritesIntent : UiIntent {
    data class RemoveFavorite(
        val photoId: String,
    ) : FavoritesIntent
}
