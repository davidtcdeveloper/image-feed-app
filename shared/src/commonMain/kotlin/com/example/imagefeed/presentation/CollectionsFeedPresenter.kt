package com.example.imagefeed.presentation

import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.CommonFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionsFeedState(
    val collections: List<PhotoCollection> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasReachedEnd: Boolean = false
)

class CollectionsFeedPresenter(
    private val repository: UnsplashRepository,
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(CollectionsFeedState())
    val state: StateFlow<CollectionsFeedState> = _state.asStateFlow()
    val iosState: CommonFlow<CollectionsFeedState> = CommonFlow(state)

    init {
        loadNextPage()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return

        _state.update { it.copy(isRefreshing = true, error = null) }

        presenterScope.launch {
            try {
                val freshCollections = repository.getCollections(page = 1, perPage = 10)
                _state.update {
                    it.copy(
                        collections = freshCollections,
                        isLoading = false,
                        isRefreshing = false,
                        page = 1,
                        hasReachedEnd = freshCollections.size < 10,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh collections"
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isRefreshing || currentState.hasReachedEnd) return

        _state.update { it.copy(isLoading = true, error = null) }

        presenterScope.launch {
            try {
                val nextPage = if (currentState.collections.isEmpty()) 1 else currentState.page + 1
                val newCollections = repository.getCollections(page = nextPage, perPage = 10)

                _state.update {
                    it.copy(
                        collections = it.collections + newCollections,
                        isLoading = false,
                        page = nextPage,
                        hasReachedEnd = newCollections.size < 10,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load collections"
                    )
                }
            }
        }
    }
}
