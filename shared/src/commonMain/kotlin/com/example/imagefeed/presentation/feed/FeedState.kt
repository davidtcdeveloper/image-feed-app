package com.example.imagefeed.presentation.feed

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.Topic
import com.example.imagefeed.presentation.mvi.UiState

data class FeedState(
    val photos: List<Photo> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val selectedTopicSlug: String = "editorial",
    val isLoading: Boolean = false,
    val isLoadingTopics: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasReachedEnd: Boolean = false,
) : UiState
