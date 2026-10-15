package com.example.imagefeed.presentation.feed

import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface FeedIntent : UiIntent {
    data object LoadTopics : FeedIntent

    data class SelectTopic(
        val slug: String,
    ) : FeedIntent

    data object LoadNextPage : FeedIntent

    data object Refresh : FeedIntent

    data class TrackDownload(
        val photo: Photo,
    ) : FeedIntent
}
