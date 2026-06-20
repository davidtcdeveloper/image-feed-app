package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.CommonFlow
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoDetailsState(
    val photo: Photo? = null,
    val stats: PhotoStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class PhotoDetailsPresenter(
    private val repository: UnsplashRepository,
    @Assisted private val photoId: String,
    private val presenterScopeFactory: PresenterScopeFactory,
) {
    private val presenterScope: PresenterScope = presenterScopeFactory.create()
    @AssistedFactory
    interface Factory {
        fun create(photoId: String): PhotoDetailsPresenter
    }

    private val _state = MutableStateFlow(PhotoDetailsState())
    val state: StateFlow<PhotoDetailsState> = _state.asStateFlow()
    val iosState: CommonFlow<PhotoDetailsState> = CommonFlow(state)

    init {
        loadDetails()
    }

    fun clear() {
        presenterScope.clear()
    }

    private fun isActive(): Boolean = presenterScope.coroutineContext.isActive()

    fun loadDetails() {
        _state.update { it.copy(isLoading = true, error = null) }

        presenterScope.launch {
            if (!isActive()) return@launch
            try {
                // Fetch photo details and statistics from Unsplash API
                val details = repository.getPhotoDetails(photoId)
                val stats = repository.getPhotoStats(photoId)

                _state.updateIfActive(coroutineContext) {
                    it.copy(
                        photo = details,
                        stats = stats,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.updateIfActive(coroutineContext) {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load photo details",
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
