package com.example.imagefeed.presentation.random

import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface RandomPhotoEffect : UiEffect {
    data class PhotoFetched(
        val photoId: String,
    ) : RandomPhotoEffect

    data class FetchFailed(
        val error: String,
    ) : RandomPhotoEffect
}
