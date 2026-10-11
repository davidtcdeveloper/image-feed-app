package com.example.imagefeed.presentation.details

import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface PhotoDetailsEffect : UiEffect {
    data object DismissScreen : PhotoDetailsEffect

    data class ShowErrorSnackbar(
        val message: String,
    ) : PhotoDetailsEffect
}
