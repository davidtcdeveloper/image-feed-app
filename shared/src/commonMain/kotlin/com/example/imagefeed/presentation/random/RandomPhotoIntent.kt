package com.example.imagefeed.presentation.random

import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface RandomPhotoIntent : UiIntent {
    data class FetchRandomPhoto(
        val query: String? = null,
        val orientation: String? = null,
    ) : RandomPhotoIntent

    data object TrackDownload : RandomPhotoIntent
}
