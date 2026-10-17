package com.example.imagefeed.presentation.collections

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.collections.data.CollectionsRepository
import com.example.imagefeed.core.network.filterDistinctAgainst
import com.example.imagefeed.core.network.toAppError
import com.example.imagefeed.presentation.mvi.MviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Inject
class CollectionsFeedViewModel(
    private val repository: CollectionsRepository,
) : MviViewModel<CollectionsFeedState, CollectionsFeedIntent, CollectionsFeedEffect>(CollectionsFeedState()) {
    private var pageJob: Job? = null

    init {
        dispatch(CollectionsFeedIntent.LoadInitial)
    }

    override fun dispatch(intent: CollectionsFeedIntent) {
        when (intent) {
            is CollectionsFeedIntent.LoadInitial -> loadNextPage()
            is CollectionsFeedIntent.LoadNextPage -> loadNextPage()
            is CollectionsFeedIntent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        if (state.value.isRefreshing) return

        pageJob?.cancel()
        updateState { it.copy(isRefreshing = true, error = null) }

        pageJob =
            viewModelScope.launch {
                try {
                    val freshCollections = repository.getCollections(page = 1, perPage = 10)
                    val distinctCollections = freshCollections.distinctBy { col -> col.id }
                    updateState {
                        it.copy(
                            collections = distinctCollections,
                            isLoading = false,
                            isRefreshing = false,
                            page = 1,
                            hasReachedEnd = freshCollections.size < 10,
                            error = null,
                        )
                    }
                    sendEffect(CollectionsFeedEffect.ScrollToTop)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val appError = e.toAppError()
                    updateState {
                        it.copy(
                            isRefreshing = false,
                            error = appError,
                        )
                    }
                    sendEffect(CollectionsFeedEffect.ShowError(appError))
                }
            }
    }

    private fun loadNextPage() {
        val currentState = state.value
        if (currentState.isLoading || currentState.isRefreshing || currentState.hasReachedEnd) return

        updateState { it.copy(isLoading = true, error = null) }

        pageJob =
            viewModelScope.launch {
                try {
                    val nextPage = if (currentState.collections.isEmpty()) 1 else currentState.page + 1
                    val newCollections = repository.getCollections(page = nextPage, perPage = 10)

                    updateState { current ->
                        val uniqueNew = newCollections.filterDistinctAgainst(current.collections) { col -> col.id }
                        current.copy(
                            collections = current.collections + uniqueNew,
                            isLoading = false,
                            page = nextPage,
                            hasReachedEnd = newCollections.size < 10 || (newCollections.isNotEmpty() && uniqueNew.isEmpty()),
                            error = null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val appError = e.toAppError()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = appError,
                        )
                    }
                    sendEffect(CollectionsFeedEffect.ShowError(appError))
                }
            }
    }

    override fun teardown() {
        super.teardown()
        pageJob?.cancel()
    }
}
