package com.example.imagefeed.presentation.collection

import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface CollectionDetailIntent : UiIntent {
    data object LoadCollection : CollectionDetailIntent

    data object LoadNextPage : CollectionDetailIntent

    data object Refresh : CollectionDetailIntent

    data class TrackDownload(
        val photo: Photo,
    ) : CollectionDetailIntent
}
