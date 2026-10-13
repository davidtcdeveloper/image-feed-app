package com.example.imagefeed.presentation.search

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.presentation.mvi.MviViewModel
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.filterDistinctAgainst
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Inject
class UnifiedSearchViewModel(
    private val repository: UnsplashRepository,
) : MviViewModel<UnifiedSearchState, UnifiedSearchIntent, UnifiedSearchEffect>(UnifiedSearchState()) {
    private var searchJob: Job? = null
    private var pageJob: Job? = null

    override fun dispatch(intent: UnifiedSearchIntent) {
        when (intent) {
            is UnifiedSearchIntent.UpdateQuery -> updateQuery(intent.query)
            is UnifiedSearchIntent.SelectSearchType -> setTab(intent.type)
            is UnifiedSearchIntent.SelectColor -> applyFilters(state.value.filters.copy(color = intent.color))
            is UnifiedSearchIntent.SelectOrientation -> applyFilters(state.value.filters.copy(orientation = intent.orientation))
            is UnifiedSearchIntent.ApplyFilters -> applyFilters(intent.filters)
            is UnifiedSearchIntent.SetSearchActive -> setSearchActive(intent.active)
            is UnifiedSearchIntent.SubmitSearch -> submitSearch(intent.query)
            is UnifiedSearchIntent.LoadNextPage -> loadNextPage()
            is UnifiedSearchIntent.ClearSearch -> clearSearch()
            is UnifiedSearchIntent.Refresh -> refresh()
            is UnifiedSearchIntent.AddHistoryEntry -> addHistoryEntry(intent.entry)
            is UnifiedSearchIntent.RemoveHistoryEntry -> removeHistoryEntry(intent.entry)
            is UnifiedSearchIntent.ClearHistory -> clearHistory()
        }
    }

    private fun setSearchActive(active: Boolean) {
        if (state.value.isSearchActive == active) return
        updateState { it.copy(isSearchActive = active) }
    }

    private fun submitSearch(query: String) {
        updateState { it.copy(isSearchActive = false, query = query) }
        sendEffect(UnifiedSearchEffect.DismissKeyboard)
        searchJob?.cancel()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        performSearch(query, reset = true)
    }

    private fun updateQuery(newQuery: String) {
        updateState { it.copy(query = newQuery) }

        searchJob?.cancel()
        if (newQuery.isBlank()) {
            clearSearch()
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(300.milliseconds)
                performSearch(newQuery, reset = true)
            }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        pageJob?.cancel()
        updateState {
            it.copy(
                query = "",
                photos = emptyList(),
                collections = emptyList(),
                users = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                hasReachedEnd = false,
                error = null,
            )
        }
    }

    private fun setTab(tab: SearchTab) {
        if (state.value.activeTab == tab) return
        updateState { it.copy(activeTab = tab, error = null, hasReachedEnd = false) }

        val query = state.value.query
        if (query.isNotBlank()) {
            val shouldSearch =
                when (tab) {
                    SearchTab.PHOTOS -> state.value.photos.isEmpty()
                    SearchTab.COLLECTIONS -> state.value.collections.isEmpty()
                    SearchTab.USERS -> state.value.users.isEmpty()
                }
            if (shouldSearch) {
                performSearch(query, reset = true)
            }
        }
    }

    private fun applyFilters(filters: SearchFilters) {
        updateState { it.copy(filters = filters) }
        val query = state.value.query
        if (query.isNotBlank()) {
            performSearch(query, reset = true)
        }
    }

    private fun loadNextPage() {
        val currentState = state.value
        if (currentState.isLoading ||
            currentState.isLoadingMore ||
            currentState.isRefreshing ||
            currentState.hasReachedEnd ||
            currentState.query.isBlank()
        ) {
            return
        }

        updateState { it.copy(isLoadingMore = true) }

        pageJob?.cancel()
        pageJob =
            viewModelScope.launch {
                try {
                    val query = currentState.query
                    when (currentState.activeTab) {
                        SearchTab.PHOTOS -> {
                            val nextPage = currentState.photoPage + 1
                            val response =
                                repository.searchPhotos(
                                    query = query,
                                    page = nextPage,
                                    perPage = 15,
                                    orderBy = currentState.filters.orderBy,
                                    color = currentState.filters.color,
                                    orientation = currentState.filters.orientation,
                                    contentFilter = currentState.filters.contentFilter,
                                )
                            updateState { current ->
                                val uniqueNew = response.results.filterDistinctAgainst(current.photos) { photo -> photo.id }
                                current.copy(
                                    photos = current.photos + uniqueNew,
                                    photoPage = nextPage,
                                    hasReachedEnd =
                                        response.results.isEmpty() ||
                                            nextPage >= response.totalPages ||
                                            (response.results.isNotEmpty() && uniqueNew.isEmpty()),
                                    isLoadingMore = false,
                                )
                            }
                        }
                        SearchTab.COLLECTIONS -> {
                            val nextPage = currentState.collectionPage + 1
                            val response = repository.searchCollections(query, nextPage, 15)
                            updateState { current ->
                                val uniqueNew = response.results.filterDistinctAgainst(current.collections) { col -> col.id }
                                current.copy(
                                    collections = current.collections + uniqueNew,
                                    collectionPage = nextPage,
                                    hasReachedEnd =
                                        response.results.isEmpty() ||
                                            nextPage >= response.totalPages ||
                                            (response.results.isNotEmpty() && uniqueNew.isEmpty()),
                                    isLoadingMore = false,
                                )
                            }
                        }
                        SearchTab.USERS -> {
                            val nextPage = currentState.userPage + 1
                            val response = repository.searchUsers(query, nextPage, 15)
                            updateState { current ->
                                val uniqueNew = response.results.filterDistinctAgainst(current.users) { user -> user.id }
                                current.copy(
                                    users = current.users + uniqueNew,
                                    userPage = nextPage,
                                    hasReachedEnd =
                                        response.results.isEmpty() ||
                                            nextPage >= response.totalPages ||
                                            (response.results.isNotEmpty() && uniqueNew.isEmpty()),
                                    isLoadingMore = false,
                                )
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load more results"
                    updateState {
                        it.copy(
                            isLoadingMore = false,
                            error = message,
                        )
                    }
                    sendEffect(UnifiedSearchEffect.ShowError(message))
                }
            }
    }

    private fun addHistoryEntry(entry: String) {
        if (entry.isBlank()) return
        updateState {
            val updated = (listOf(entry) + it.searchHistory.filter { item -> item != entry }).take(8)
            it.copy(searchHistory = updated)
        }
    }

    private fun removeHistoryEntry(entry: String) {
        updateState {
            it.copy(searchHistory = it.searchHistory.filter { item -> item != entry })
        }
    }

    private fun clearHistory() {
        updateState { it.copy(searchHistory = emptyList()) }
    }

    private fun refresh() {
        val currentState = state.value
        val query = currentState.query
        if (query.isBlank() || currentState.isRefreshing || currentState.isLoading || currentState.isLoadingMore) return

        updateState { it.copy(isRefreshing = true, error = null) }

        viewModelScope.launch {
            try {
                when (currentState.activeTab) {
                    SearchTab.PHOTOS -> {
                        val response =
                            repository.searchPhotos(
                                query = query,
                                page = 1,
                                perPage = 15,
                                orderBy = currentState.filters.orderBy,
                                color = currentState.filters.color,
                                orientation = currentState.filters.orientation,
                                contentFilter = currentState.filters.contentFilter,
                            )
                        updateState { current ->
                            if (current.query != query) return@updateState current.copy(isRefreshing = false)
                            val distinctPhotos = response.results.distinctBy { photo -> photo.id }
                            current.copy(
                                photos = distinctPhotos,
                                photoPage = 1,
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isRefreshing = false,
                                error = null,
                            )
                        }
                        sendEffect(UnifiedSearchEffect.ScrollToTop)
                    }
                    SearchTab.COLLECTIONS -> {
                        val response = repository.searchCollections(query, 1, 15)
                        updateState { current ->
                            if (current.query != query) return@updateState current.copy(isRefreshing = false)
                            val distinctCollections = response.results.distinctBy { col -> col.id }
                            current.copy(
                                collections = distinctCollections,
                                collectionPage = 1,
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isRefreshing = false,
                                error = null,
                            )
                        }
                        sendEffect(UnifiedSearchEffect.ScrollToTop)
                    }
                    SearchTab.USERS -> {
                        val response = repository.searchUsers(query, 1, 15)
                        updateState { current ->
                            if (current.query != query) return@updateState current.copy(isRefreshing = false)
                            val distinctUsers = response.results.distinctBy { user -> user.id }
                            current.copy(
                                users = distinctUsers,
                                userPage = 1,
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isRefreshing = false,
                                error = null,
                            )
                        }
                        sendEffect(UnifiedSearchEffect.ScrollToTop)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Failed to refresh search"
                updateState {
                    it.copy(
                        isRefreshing = false,
                        error = message,
                    )
                }
                sendEffect(UnifiedSearchEffect.ShowError(message))
            }
        }
    }

    private fun performSearch(
        query: String,
        reset: Boolean,
    ) {
        addHistoryEntry(query)

        updateState {
            it.copy(
                isLoading = reset,
                error = null,
                photoPage = if (reset) 1 else it.photoPage,
                collectionPage = if (reset) 1 else it.collectionPage,
                userPage = if (reset) 1 else it.userPage,
                photos = if (reset) emptyList() else it.photos,
                collections = if (reset) emptyList() else it.collections,
                users = if (reset) emptyList() else it.users,
            )
        }

        viewModelScope.launch {
            try {
                when (state.value.activeTab) {
                    SearchTab.PHOTOS -> {
                        val response =
                            repository.searchPhotos(
                                query = query,
                                page = 1,
                                perPage = 15,
                                orderBy = state.value.filters.orderBy,
                                color = state.value.filters.color,
                                orientation = state.value.filters.orientation,
                                contentFilter = state.value.filters.contentFilter,
                            )
                        updateState {
                            it.copy(
                                photos = response.results.distinctBy { photo -> photo.id },
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isLoading = false,
                            )
                        }
                    }
                    SearchTab.COLLECTIONS -> {
                        val response = repository.searchCollections(query, 1, 15)
                        updateState {
                            it.copy(
                                collections = response.results.distinctBy { col -> col.id },
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isLoading = false,
                            )
                        }
                    }
                    SearchTab.USERS -> {
                        val response = repository.searchUsers(query, 1, 15)
                        updateState {
                            it.copy(
                                users = response.results.distinctBy { user -> user.id },
                                hasReachedEnd = response.results.isEmpty() || response.totalPages <= 1,
                                isLoading = false,
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Search failed"
                updateState {
                    it.copy(
                        isLoading = false,
                        error = message,
                    )
                }
                sendEffect(UnifiedSearchEffect.ShowError(message))
            }
        }
    }

    override fun teardown() {
        super.teardown()
        searchJob?.cancel()
        pageJob?.cancel()
    }
}
