package com.example.imagefeed.presentation.search

import com.example.imagefeed.model.AppError
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.mvi.UiState

enum class SearchTab { PHOTOS, COLLECTIONS, USERS }

data class SearchFilters(
    val orderBy: String = "relevant",
    val color: String? = null,
    val orientation: String? = null,
    val contentFilter: String = "low",
) {
    constructor() : this("relevant", null, null, "low")
}

data class UnifiedSearchState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val isRefreshing: Boolean = false,
    val activeTab: SearchTab = SearchTab.PHOTOS,
    val filters: SearchFilters = SearchFilters(),
    val photos: List<Photo> = emptyList(),
    val collections: List<CollectionSummary> = emptyList(),
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
    val searchHistory: List<String> = emptyList(),
    val photoPage: Int = 1,
    val collectionPage: Int = 1,
    val userPage: Int = 1,
    val hasReachedEnd: Boolean = false,
) : UiState

typealias SearchState = UnifiedSearchState
