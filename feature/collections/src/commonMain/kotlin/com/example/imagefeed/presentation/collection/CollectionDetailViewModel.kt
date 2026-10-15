package com.example.imagefeed.presentation.collection

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.collections.data.CollectionsRepository
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.core.network.filterDistinctAgainst
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.MviViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AssistedInject
class CollectionDetailViewModel(
    @Assisted private val collectionId: String,
    private val repository: CollectionsRepository,
    private val downloadTracker: PhotoDownloadTracker,
) : MviViewModel<CollectionDetailState, CollectionDetailIntent, CollectionDetailEffect>(CollectionDetailState()) {
    @AssistedFactory
    interface Factory {
        fun create(collectionId: String): CollectionDetailViewModel
    }

    private var headerJob: Job? = null
    private var photosJob: Job? = null

    init {
        dispatch(CollectionDetailIntent.LoadCollection)
        dispatch(CollectionDetailIntent.LoadNextPage)
    }

    override fun dispatch(intent: CollectionDetailIntent) {
        when (intent) {
            is CollectionDetailIntent.LoadCollection -> loadHeaderAndRelated()
            is CollectionDetailIntent.LoadNextPage -> loadNextPhotosPage()
            is CollectionDetailIntent.Refresh -> refresh()
            is CollectionDetailIntent.TrackDownload -> trackDownload(intent.photo)
        }
    }

    private fun loadHeaderAndRelated() {
        headerJob?.cancel()
        updateState { it.copy(isHeaderLoading = true, error = null) }
        headerJob =
            viewModelScope.launch {
                try {
                    val collectionDetails = repository.getCollection(collectionId)
                    updateState { it.copy(collection = collectionDetails) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load collection details"
                    updateState { it.copy(error = message) }
                    sendEffect(CollectionDetailEffect.ShowError(message))
                }

                try {
                    val relatedCollections = repository.getRelatedCollections(collectionId)
                    updateState {
                        it.copy(
                            related = relatedCollections.distinctBy { col -> col.id },
                            isHeaderLoading = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    updateState { it.copy(isHeaderLoading = false) }
                }
            }
    }

    private fun loadNextPhotosPage() {
        val currentState = state.value
        if (currentState.isLoadingPhotos || currentState.hasReachedEnd) return

        photosJob?.cancel()
        updateState { it.copy(isLoadingPhotos = true, error = null) }
        photosJob =
            viewModelScope.launch {
                try {
                    val nextPage = if (currentState.photos.isEmpty()) 1 else currentState.photosPage + 1
                    val newPhotos = repository.getCollectionPhotos(collectionId, page = nextPage, perPage = 15)

                    updateState { current ->
                        val uniqueNewPhotos = newPhotos.filterDistinctAgainst(current.photos) { photo -> photo.id }
                        current.copy(
                            photos = current.photos + uniqueNewPhotos,
                            isLoadingPhotos = false,
                            photosPage = nextPage,
                            hasReachedEnd = newPhotos.size < 15 || (newPhotos.isNotEmpty() && uniqueNewPhotos.isEmpty()),
                            error = null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load collection photos"
                    updateState {
                        it.copy(
                            isLoadingPhotos = false,
                            error = message,
                        )
                    }
                    sendEffect(CollectionDetailEffect.ShowError(message))
                }
            }
    }

    private fun refresh() {
        updateState {
            it.copy(
                photos = emptyList(),
                photosPage = 1,
                hasReachedEnd = false,
                error = null,
            )
        }
        loadHeaderAndRelated()
        loadNextPhotosPage()
    }

    private fun trackDownload(photo: Photo) {
        viewModelScope.launch {
            downloadTracker.trackDownload(photo.links.downloadLocation)
        }
    }

    override fun teardown() {
        super.teardown()
        headerJob?.cancel()
        photosJob?.cancel()
    }
}
