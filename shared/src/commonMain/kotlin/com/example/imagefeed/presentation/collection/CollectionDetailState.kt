package com.example.imagefeed.presentation.collection

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.presentation.mvi.UiState

data class CollectionDetailState(
    val collection: PhotoCollection? = null,
    val photos: List<Photo> = emptyList(),
    val related: List<PhotoCollection> = emptyList(),
    val isLoadingPhotos: Boolean = false,
    val isHeaderLoading: Boolean = false,
    val error: String? = null,
    val photosPage: Int = 1,
    val hasReachedEnd: Boolean = false,
) : UiState
