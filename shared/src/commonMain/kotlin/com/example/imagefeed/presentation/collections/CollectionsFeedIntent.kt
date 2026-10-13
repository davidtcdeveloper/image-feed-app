package com.example.imagefeed.presentation.collections

import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface CollectionsFeedIntent : UiIntent {
    data object LoadInitial : CollectionsFeedIntent

    data object LoadNextPage : CollectionsFeedIntent

    data object Refresh : CollectionsFeedIntent
}
