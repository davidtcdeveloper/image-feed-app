package com.example.imagefeed.presentation.favorites

import com.example.imagefeed.model.AppError
import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface FavoritesEffect : UiEffect {
    data object ShowFavoriteRemovedSnackbar : FavoritesEffect

    data class ShowErrorSnackbar(
        val error: AppError,
    ) : FavoritesEffect
}
