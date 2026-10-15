package com.example.imagefeed.presentation.details

import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface PhotoDetailsIntent : UiIntent {
    data object LoadDetails : PhotoDetailsIntent

    data class TrackDownload(
        val photo: Photo,
    ) : PhotoDetailsIntent

    data object Retry : PhotoDetailsIntent
}
