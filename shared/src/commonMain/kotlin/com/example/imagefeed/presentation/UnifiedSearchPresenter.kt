package com.example.imagefeed.presentation

import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.CommonFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SearchTab { PHOTOS, COLLECTIONS, USERS }

data class SearchFilters(
    val orderBy: String = "relevant",
    val color: String? = null,
    val orientation: String? = null,
    val contentFilter: String = "low"
)

data class SearchState(
    val query: String = "",
    val activeTab: SearchTab = SearchTab.PHOTOS,
    val filters: SearchFilters = SearchFilters(),
    val photos: List<Photo> = emptyList(),
    val collections: List<CollectionSummary> = emptyList(),
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchHistory: List<String> = listOf("nature", "wallpapers", "minimalist", "neon", "architecture"),
    val photoPage: Int = 1,
    val collectionPage: Int = 1,
    val userPage: Int = 1,
    val hasReachedEnd: Boolean = false
)

class UnifiedSearchPresenter(
    private val repository: UnsplashRepository,
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    val iosState: CommonFlow<SearchState> = CommonFlow(state)

    private var searchJob: Job? = null
    private val defaultSuggestions = listOf("nature", "travel", "minimalist", "urban", "vintage", "people", "neon", "textures")

    fun updateQuery(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _state.update {
                it.copy(
                    photos = emptyList(),
                    collections = emptyList(),
                    users = emptyList(),
                    isLoading = false,
                    hasReachedEnd = false
                )
            }
            return
        }

        searchJob = presenterScope.launch {
            delay(300) // Debounce search requests
            performSearch(newQuery, reset = true)
        }
    }

    fun setTab(tab: SearchTab) {
        if (_state.value.activeTab == tab) return
        _state.update { it.copy(activeTab = tab, error = null, hasReachedEnd = false) }
        
        // Trigger search on tab switch if query exists and no data is loaded yet
        val query = _state.value.query
        if (query.isNotBlank()) {
            val shouldSearch = when (tab) {
                SearchTab.PHOTOS -> _state.value.photos.isEmpty()
                SearchTab.COLLECTIONS -> _state.value.collections.isEmpty()
                SearchTab.USERS -> _state.value.users.isEmpty()
            }
            if (shouldSearch) {
                performSearch(query, reset = true)
            }
        }
    }

    fun applyFilters(filters: SearchFilters) {
        _state.update { it.copy(filters = filters) }
        val query = _state.value.query
        if (query.isNotBlank()) {
            performSearch(query, reset = true)
        }
    }

    fun loadNextPage() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isLoadingMore || currentState.hasReachedEnd || currentState.query.isBlank()) return

        _state.update { it.copy(isLoadingMore = true) }
        
        presenterScope.launch {
            try {
                val query = currentState.query
                when (currentState.activeTab) {
                    SearchTab.PHOTOS -> {
                        val nextPage = currentState.photoPage + 1
                        val response = repository.searchPhotos(
                            query = query,
                            page = nextPage,
                            perPage = 15,
                            orderBy = currentState.filters.orderBy,
                            color = currentState.filters.color,
                            orientation = currentState.filters.orientation,
                            contentFilter = currentState.filters.contentFilter
                        )
                        _state.update {
                            it.copy(
                                photos = it.photos + response.results,
                                photoPage = nextPage,
                                hasReachedEnd = response.results.isEmpty() || nextPage >= response.totalPages,
                                isLoadingMore = false
                            )
                        }
                    }
                    SearchTab.COLLECTIONS -> {
                        val nextPage = currentState.collectionPage + 1
                        val response = repository.searchCollections(query, nextPage, 15)
                        _state.update {
                            it.copy(
                                collections = it.collections + response.results,
                                collectionPage = nextPage,
                                hasReachedEnd = response.results.isEmpty() || nextPage >= response.totalPages,
                                isLoadingMore = false
                            )
                        }
                    }
                    SearchTab.USERS -> {
                        val nextPage = currentState.userPage + 1
                        val response = repository.searchUsers(query, nextPage, 15)
                        _state.update {
                            it.copy(
                                users = it.users + response.results,
                                userPage = nextPage,
                                hasReachedEnd = response.results.isEmpty() || nextPage >= response.totalPages,
                                isLoadingMore = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load more results"
                    )
                }
            }
        }
    }

    fun addHistoryEntry(entry: String) {
        if (entry.isBlank()) return
        _state.update {
            val updated = (listOf(entry) + it.searchHistory.filter { item -> item != entry }).take(8)
            it.copy(searchHistory = updated)
        }
    }

    fun removeHistoryEntry(entry: String) {
        _state.update {
            it.copy(searchHistory = it.searchHistory.filter { item -> item != entry })
        }
    }

    fun clearHistory() {
        _state.update { it.copy(searchHistory = emptyList()) }
    }

    private fun performSearch(query: String, reset: Boolean) {
        addHistoryEntry(query)
        
        _state.update { 
            it.copy(
                isLoading = reset, 
                error = null,
                photoPage = if (reset) 1 else it.photoPage,
                collectionPage = if (reset) 1 else it.collectionPage,
                userPage = if (reset) 1 else it.userPage,
                photos = if (reset) emptyList() else it.photos,
                collections = if (reset) emptyList() else it.collections,
                users = if (reset) emptyList() else it.users
            ) 
        }

        presenterScope.launch {
            try {
                when (_state.value.activeTab) {
                    SearchTab.PHOTOS -> {
                        val response = repository.searchPhotos(
                            query = query,
                            page = 1,
                            perPage = 15,
                            orderBy = _state.value.filters.orderBy,
                            color = _state.value.filters.color,
                            orientation = _state.value.filters.orientation,
                            contentFilter = _state.value.filters.contentFilter
                        )
                        _state.update {
                            it.copy(
                                photos = response.results,
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isLoading = false
                            )
                        }
                    }
                    SearchTab.COLLECTIONS -> {
                        val response = repository.searchCollections(query, 1, 15)
                        _state.update {
                            it.copy(
                                collections = response.results,
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isLoading = false
                            )
                        }
                    }
                    SearchTab.USERS -> {
                        val response = repository.searchUsers(query, 1, 15)
                        _state.update {
                            it.copy(
                                users = response.results,
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Search failed"
                    )
                }
            }
        }
    }
}
