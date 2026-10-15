package com.example.imagefeed.presentation.details

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.MviViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AssistedInject
class PhotoDetailsViewModel(
    @Assisted private val photoId: String,
    private val repository: PhotoDetailsRepository,
    private val downloadTracker: PhotoDownloadTracker,
) : MviViewModel<PhotoDetailsState, PhotoDetailsIntent, PhotoDetailsEffect>(PhotoDetailsState()) {
    @AssistedFactory
    interface Factory {
        fun create(photoId: String): PhotoDetailsViewModel
    }

    private var loadJob: Job? = null

    init {
        dispatch(PhotoDetailsIntent.LoadDetails)
    }

    override fun dispatch(intent: PhotoDetailsIntent) {
        when (intent) {
            is PhotoDetailsIntent.LoadDetails -> loadDetails()
            is PhotoDetailsIntent.Retry -> loadDetails()
            is PhotoDetailsIntent.TrackDownload -> trackDownload(intent.photo)
        }
    }

    private fun loadDetails() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, error = null) }
        loadJob =
            viewModelScope.launch {
                try {
                    val details = repository.getPhotoDetails(photoId)
                    val stats = repository.getPhotoStats(photoId)
                    updateState {
                        it.copy(
                            photo = details,
                            stats = stats,
                            isLoading = false,
                            error = null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Failed to load photo details"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorMessage,
                        )
                    }
                    sendEffect(PhotoDetailsEffect.ShowErrorSnackbar(errorMessage))
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
        loadJob?.cancel()
    }
}
