package com.example.imagefeed.presentation.feed

import com.example.imagefeed.presentation.mvi.UiEffect

sealed interface FeedEffect : UiEffect {
    data object ScrollToTop : FeedEffect

    data class ShowToast(
        val message: String,
    ) : FeedEffect
}
