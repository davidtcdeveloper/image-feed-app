package com.example.imagefeed.presentation.details

import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiState

data class PhotoDetailsState(
    val photo: Photo? = null,
    val stats: PhotoStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) : UiState
