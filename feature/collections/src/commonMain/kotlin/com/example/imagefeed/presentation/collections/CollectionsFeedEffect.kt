package com.example.imagefeed.presentation.collections

import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface CollectionsFeedEffect : UiEffect {
    data object ScrollToTop : CollectionsFeedEffect

    data class ShowError(
        val message: String,
    ) : CollectionsFeedEffect
}
