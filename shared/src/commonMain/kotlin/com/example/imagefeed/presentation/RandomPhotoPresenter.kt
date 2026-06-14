package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.CommonFlow
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _state = MutableStateFlow(RandomPhotoState())
    val state: StateFlow<RandomPhotoState> = _state.asStateFlow()
    val iosState: CommonFlow<RandomPhotoState> = CommonFlow(state)

    init {
        loadRandomPhoto()
    }

    fun loadRandomPhoto(
        query: String? = null,
        orientation: String? = null,
    ) {
        _state.update { it.copy(isLoading = true, error = null) }

        presenterScope.launch {
            try {
                // Fetch a single random photo (returns a list of 1 photo when count = 1)
                val photos = repository.getRandomPhotos(orientation = orientation, query = query, count = 1)
                _state.update {
                    it.copy(
                        photo = photos.firstOrNull(),
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
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
                repository.trackDownload(photo.links.downloadLocation)
            }
        }
    }
}
