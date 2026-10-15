package com.example.imagefeed.presentation.feed

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.core.network.filterDistinctAgainst
import com.example.imagefeed.feed.data.FeedRepository
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.MviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Inject
class FeedViewModel(
    private val repository: FeedRepository,
    private val downloadTracker: PhotoDownloadTracker,
) : MviViewModel<FeedState, FeedIntent, FeedEffect>(FeedState()) {
    private var pageJob: Job? = null
    private var topicsJob: Job? = null

    init {
        dispatch(FeedIntent.LoadTopics)
        dispatch(FeedIntent.LoadNextPage)
    }

    override fun dispatch(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.LoadTopics -> loadTopics()
            is FeedIntent.SelectTopic -> selectTopic(intent.slug)
            is FeedIntent.LoadNextPage -> loadNextPage()
            is FeedIntent.Refresh -> refresh()
            is FeedIntent.TrackDownload -> trackDownload(intent.photo)
        }
    }

    private fun loadTopics() {
        if (state.value.isLoadingTopics) return
        topicsJob?.cancel()
        updateState { it.copy(isLoadingTopics = true) }
        topicsJob =
            viewModelScope.launch {
                try {
                    val list = repository.getTopics(page = 1, perPage = 25)
                    updateState {
                        it.copy(
                            topics = list,
                            isLoadingTopics = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load categories"
                    updateState {
                        it.copy(
                            isLoadingTopics = false,
                            error = message,
                        )
                    }
                    sendEffect(FeedEffect.ShowToast(message))
                }
            }
    }

    private fun selectTopic(slug: String) {
        val current = state.value
        if (current.selectedTopicSlug == slug) return

        pageJob?.cancel()

        updateState {
            it.copy(
                selectedTopicSlug = slug,
                photos = emptyList(),
                page = 1,
                hasReachedEnd = false,
                isLoading = false,
                error = null,
            )
        }

        loadNextPage()
    }

    private fun refresh() {
        if (state.value.isRefreshing) return

        pageJob?.cancel()
        updateState { it.copy(isRefreshing = true, error = null) }

        pageJob =
            viewModelScope.launch {
                try {
                    val slug = state.value.selectedTopicSlug
                    val freshPhotos =
                        if (slug == "editorial") {
                            repository.getPhotos(page = 1, perPage = 15)
                        } else {
                            repository.getTopicPhotos(slug, page = 1, perPage = 15)
                        }

                    updateState { current ->
                        if (current.selectedTopicSlug != slug) return@updateState current
                        val distinctPhotos = freshPhotos.distinctBy { photo -> photo.id }
                        current.copy(
                            photos = distinctPhotos,
                            isLoading = false,
                            isRefreshing = false,
                            page = 1,
                            hasReachedEnd = freshPhotos.size < 15,
                            error = null,
                        )
                    }
                    sendEffect(FeedEffect.ScrollToTop)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to refresh feed"
                    updateState {
                        it.copy(
                            isRefreshing = false,
                            error = message,
                        )
                    }
                    sendEffect(FeedEffect.ShowToast(message))
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
                    val slug = currentState.selectedTopicSlug
                    val nextPage = if (currentState.photos.isEmpty()) 1 else currentState.page + 1

                    val newPhotos =
                        if (slug == "editorial") {
                            repository.getPhotos(page = nextPage, perPage = 15)
                        } else {
                            repository.getTopicPhotos(slug, page = nextPage, perPage = 15)
                        }

                    updateState { current ->
                        if (current.selectedTopicSlug != slug) return@updateState current
                        val uniqueNewPhotos = newPhotos.filterDistinctAgainst(current.photos) { photo -> photo.id }
                        current.copy(
                            photos = current.photos + uniqueNewPhotos,
                            isLoading = false,
                            page = nextPage,
                            hasReachedEnd = newPhotos.size < 15 || (newPhotos.isNotEmpty() && uniqueNewPhotos.isEmpty()),
                            error = null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load photos"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = message,
                        )
                    }
                    sendEffect(FeedEffect.ShowToast(message))
                }
            }
    }

    private fun trackDownload(photo: Photo) {
        viewModelScope.launch {
            downloadTracker.trackDownload(photo.links.downloadLocation)
        }
    }

    override fun teardown() {
        super.teardown()
        pageJob?.cancel()
        topicsJob?.cancel()
    }
}
