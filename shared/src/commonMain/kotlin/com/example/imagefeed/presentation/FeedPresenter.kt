package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.Topic
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
)

class FeedPresenter(
    private val repository: UnsplashRepository,
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()
    val iosState: CommonFlow<FeedState> = CommonFlow(state)

    init {
        loadTopics()
        loadNextPage()
    }

    fun loadTopics() {
        if (_state.value.isLoadingTopics) return
        _state.update { it.copy(isLoadingTopics = true) }
        presenterScope.launch {
            try {
                val list = repository.getTopics(page = 1, perPage = 25)
                _state.update {
                    it.copy(
                        topics = list,
                        isLoadingTopics = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingTopics = false,
                        error = e.message ?: "Failed to load categories",
                    )
                }
            }
        }
    }

    fun selectTopic(slug: String) {
        val current = _state.value
        if (current.selectedTopicSlug == slug) return

        _state.update {
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

    fun refresh() {
        if (_state.value.isRefreshing) return

        _state.update { it.copy(isRefreshing = true, error = null) }

        presenterScope.launch {
            try {
                val slug = _state.value.selectedTopicSlug
                val freshPhotos =
                    if (slug == "editorial") {
                        repository.getPhotos(page = 1, perPage = 15)
                    } else {
                        repository.getTopicPhotos(slug, page = 1, perPage = 15)
                    }

                _state.update {
                    it.copy(
                        photos = freshPhotos,
                        isLoading = false,
                        isRefreshing = false,
                        page = 1,
                        hasReachedEnd = freshPhotos.size < 15,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh feed",
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
                val slug = currentState.selectedTopicSlug
                val nextPage = if (currentState.photos.isEmpty()) 1 else currentState.page + 1

                val newPhotos =
                    if (slug == "editorial") {
                        repository.getPhotos(page = nextPage, perPage = 15)
                    } else {
                        repository.getTopicPhotos(slug, page = nextPage, perPage = 15)
                    }

                _state.update {
                    it.copy(
                        photos = it.photos + newPhotos,
                        isLoading = false,
                        page = nextPage,
                        hasReachedEnd = newPhotos.size < 15,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load photos",
                    )
                }
            }
        }
    }

    fun trackDownload(photo: Photo) {
        presenterScope.launch {
            repository.trackDownload(photo.links.downloadLocation)
        }
    }
}
