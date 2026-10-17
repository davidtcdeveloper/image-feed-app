package com.example.imagefeed.presentation.random

import com.example.imagefeed.model.AppError
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiState

data class RandomPhotoState(
    val photo: Photo? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
) : UiState
