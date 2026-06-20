package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.CommonFlow
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RandomPhotoState(
    val photo: Photo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Inject
class RandomPhotoPresenter(
    private val repository: UnsplashRepository,
    private val presenterScopeFactory: PresenterScopeFactory,
) {
    private val presenterScope: PresenterScope = presenterScopeFactory.create()
    private val _state = MutableStateFlow(RandomPhotoState())
    val state: StateFlow<RandomPhotoState> = _state.asStateFlow()
    val iosState: CommonFlow<RandomPhotoState> = CommonFlow(state)

    init {
        loadRandomPhoto()
    }

    fun clear() {
        presenterScope.clear()
    }

    private fun isActive(): Boolean = presenterScope.coroutineContext.isActive()

    fun loadRandomPhoto(
        query: String? = null,
        orientation: String? = null,
    ) {
        _state.update { it.copy(isLoading = true, error = null) }

        presenterScope.launch {
            if (!isActive()) return@launch
            try {
                // Fetch a single random photo (returns a list of 1 photo when count = 1)
                val photos = repository.getRandomPhotos(orientation = orientation, query = query, count = 1)
                _state.updateIfActive(coroutineContext) {
                    it.copy(
                        photo = photos.firstOrNull(),
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.updateIfActive(coroutineContext) {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load random photo",
                    )
                }
            }
        }
    }

    fun trackDownload() {
        _state.value.photo?.let { photo ->
            presenterScope.launch {
                if (!isActive()) return@launch
                repository.trackDownload(photo.links.downloadLocation)
            }
        }
    }
}
