package com.example.imagefeed.presentation.favorites

import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiState

data class FavoritesState(
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
) : UiState
