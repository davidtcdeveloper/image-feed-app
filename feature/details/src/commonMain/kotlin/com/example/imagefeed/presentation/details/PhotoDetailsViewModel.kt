package com.example.imagefeed.presentation.details

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.core.database.FavoritesRepository
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.core.network.toAppError
import com.example.imagefeed.details.data.PhotoDetailsRepository
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.cache.NavigationPreviewCache
import com.example.imagefeed.presentation.mvi.MviViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@AssistedInject
class PhotoDetailsViewModel(
    @Assisted private val photoId: String,
    private val repository: PhotoDetailsRepository,
    private val downloadTracker: PhotoDownloadTracker,
    private val favoritesRepository: FavoritesRepository,
    previewCache: NavigationPreviewCache = NavigationPreviewCache(),
) : MviViewModel<PhotoDetailsState, PhotoDetailsIntent, PhotoDetailsEffect>(
        PhotoDetailsState(photo = previewCache.getPhoto(photoId)),
    ) {
    @AssistedFactory
    interface Factory {
        fun create(photoId: String): PhotoDetailsViewModel
    }

    private var loadJob: Job? = null
    private var favoriteObserveJob: Job? = null

    init {
        observeFavoriteStatus()
        dispatch(PhotoDetailsIntent.LoadDetails)
    }

    override fun dispatch(intent: PhotoDetailsIntent) {
        when (intent) {
            is PhotoDetailsIntent.LoadDetails -> loadDetails()
            is PhotoDetailsIntent.Retry -> loadDetails()
            is PhotoDetailsIntent.TrackDownload -> trackDownload(intent.photo)
            is PhotoDetailsIntent.ToggleFavorite -> toggleFavorite(intent.fallbackPhoto)
        }
    }

    private fun observeFavoriteStatus() {
        favoriteObserveJob?.cancel()
        favoriteObserveJob =
            viewModelScope.launch {
                favoritesRepository
                    .observeIsFavorite(photoId)
                    .catch { e -> sendEffect(PhotoDetailsEffect.ShowErrorSnackbar(e.toAppError())) }
                    .collect { isFavorite ->
                        updateState { it.copy(isFavorite = isFavorite) }
                    }
            }
    }

    private fun loadDetails() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, error = null) }
        loadJob =
            viewModelScope.launch {
                try {
                    if (state.value.photo == null) {
                        val cachedPhoto =
                            try {
                                favoritesRepository.getFavoriteById(photoId)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                null
                            }
                        if (cachedPhoto != null) {
                            updateState { it.copy(photo = cachedPhoto) }
                        }
                    }

                    val (details, stats) =
                        coroutineScope {
                            val detailsDeferred =
                                async {
                                    val loadedDetails = repository.getPhotoDetails(photoId)
                                    updateState { it.copy(photo = loadedDetails) }
                                    loadedDetails
                                }
                            val statsDeferred = async { repository.getPhotoStats(photoId) }
                            detailsDeferred.await() to statsDeferred.await()
                        }
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
                    val appError = e.toAppError()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = appError,
                        )
                    }
                    sendEffect(PhotoDetailsEffect.ShowErrorSnackbar(appError))
                }
            }
    }

    private fun toggleFavorite(fallbackPhoto: Photo?) {
        val targetPhoto = state.value.photo ?: fallbackPhoto
        if (targetPhoto == null && !state.value.isFavorite) return

        viewModelScope.launch {
            try {
                val isFavorite =
                    if (targetPhoto != null) {
                        favoritesRepository.toggleFavorite(targetPhoto)
                    } else {
                        favoritesRepository.removeFavorite(photoId)
                        false
                    }
                updateState { it.copy(isFavorite = isFavorite) }
                sendEffect(PhotoDetailsEffect.ShowFavoriteToggledSnackbar(isFavorite))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendEffect(PhotoDetailsEffect.ShowErrorSnackbar(e.toAppError()))
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
        favoriteObserveJob?.cancel()
    }
}
