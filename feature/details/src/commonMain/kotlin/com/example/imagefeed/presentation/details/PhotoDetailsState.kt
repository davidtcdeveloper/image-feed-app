package com.example.imagefeed.presentation.details

import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.AppError
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiState

data class PhotoDetailsState(
    val photo: Photo? = null,
    val stats: PhotoStats? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null,
) : UiState
