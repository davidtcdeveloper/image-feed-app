package com.example.imagefeed.presentation.collections

import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.presentation.mvi.UiState

data class CollectionsFeedState(
    val collections: List<PhotoCollection> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasReachedEnd: Boolean = false,
) : UiState
