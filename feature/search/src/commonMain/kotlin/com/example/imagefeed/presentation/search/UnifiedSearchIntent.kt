package com.example.imagefeed.presentation.search

import com.example.imagefeed.presentation.mvi.UiIntent

sealed interface UnifiedSearchIntent : UiIntent {
    data class UpdateQuery(
        val query: String,
    ) : UnifiedSearchIntent

    data class SelectSearchType(
        val type: SearchTab,
    ) : UnifiedSearchIntent

    data class SelectColor(
        val color: String?,
    ) : UnifiedSearchIntent

    data class SelectOrientation(
        val orientation: String?,
    ) : UnifiedSearchIntent

    data class ApplyFilters(
        val filters: SearchFilters,
    ) : UnifiedSearchIntent

    data class SetSearchActive(
        val active: Boolean,
    ) : UnifiedSearchIntent

    data class SubmitSearch(
        val query: String,
    ) : UnifiedSearchIntent

    data object LoadNextPage : UnifiedSearchIntent

    data object ClearSearch : UnifiedSearchIntent

    data object Refresh : UnifiedSearchIntent

    data class AddHistoryEntry(
        val entry: String,
    ) : UnifiedSearchIntent

    data class RemoveHistoryEntry(
        val entry: String,
    ) : UnifiedSearchIntent

    data object ClearHistory : UnifiedSearchIntent
}
