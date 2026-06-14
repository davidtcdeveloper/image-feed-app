package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.repository.UnsplashRepository
import com.example.imagefeed.util.CommonFlow
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionDetailState(
    val collection: PhotoCollection? = null,
    val photos: List<Photo> = emptyList(),
    val related: List<PhotoCollection> = emptyList(),
    val isLoadingPhotos: Boolean = false,
    val isHeaderLoading: Boolean = false,
    val error: String? = null,
    val photosPage: Int = 1,
    val hasReachedEnd: Boolean = false,
)

@AssistedInject
class CollectionDetailPresenter(
    private val repository: UnsplashRepository,
    @Assisted private val collectionId: String,
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    @AssistedFactory
    interface Factory {
        fun create(collectionId: String): CollectionDetailPresenter
    }

    private val _state = MutableStateFlow(CollectionDetailState())
    val state: StateFlow<CollectionDetailState> = _state.asStateFlow()
    val iosState: CommonFlow<CollectionDetailState> = CommonFlow(state)

    init {
        loadHeaderAndRelated()
        loadNextPhotosPage()
    }

    fun loadHeaderAndRelated() {
        _state.update { it.copy(isHeaderLoading = true, error = null) }
        presenterScope.launch {
            try {
                // Fetch collection metadata
                val collectionDetails = repository.getCollection(collectionId)
                _state.update { it.copy(collection = collectionDetails) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load collection details") }
            }

            try {
                // Fetch related collections
                val relatedCollections = repository.getRelatedCollections(collectionId)
                _state.update { it.copy(related = relatedCollections, isHeaderLoading = false) }
            } catch (e: Exception) {
                // Ignore related fetch failures so it doesn't break the main details
                _state.update { it.copy(isHeaderLoading = false) }
            }
        }
    }

    fun loadNextPhotosPage() {
        val currentState = _state.value
        if (currentState.isLoadingPhotos || currentState.hasReachedEnd) return

        _state.update { it.copy(isLoadingPhotos = true, error = null) }

        presenterScope.launch {
            try {
                val nextPage = if (currentState.photos.isEmpty()) 1 else currentState.photosPage + 1
                val newPhotos = repository.getCollectionPhotos(collectionId, page = nextPage, perPage = 15)

                _state.update {
                    it.copy(
                        photos = it.photos + newPhotos,
                        isLoadingPhotos = false,
                        photosPage = nextPage,
                        hasReachedEnd = newPhotos.size < 15,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingPhotos = false,
                        error = e.message ?: "Failed to load collection photos",
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
