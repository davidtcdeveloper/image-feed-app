package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoStats
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

data class PhotoDetailsState(
    val photo: Photo? = null,
    val stats: PhotoStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PhotoDetailsPresenter(
    private val repository: UnsplashRepository,
    private val photoId: String,
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _state = MutableStateFlow(PhotoDetailsState())
    val state: StateFlow<PhotoDetailsState> = _state.asStateFlow()
    val iosState: CommonFlow<PhotoDetailsState> = CommonFlow(state)

    init {
        loadDetails()
    }

    fun loadDetails() {
        _state.update { it.copy(isLoading = true, error = null) }

        presenterScope.launch {
            try {
                // Fetch photo details and statistics from Unsplash API
                val details = repository.getPhotoDetails(photoId)
                val stats = repository.getPhotoStats(photoId)

                _state.update {
                    it.copy(
                        photo = details,
                        stats = stats,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
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
                repository.trackDownload(photo.links.downloadLocation)
            }
        }
    }
}
