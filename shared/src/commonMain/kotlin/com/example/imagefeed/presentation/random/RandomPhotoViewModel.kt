package com.example.imagefeed.presentation.random

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.presentation.mvi.MviViewModel
import com.example.imagefeed.repository.UnsplashRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Inject
class RandomPhotoViewModel(
    private val repository: UnsplashRepository,
) : MviViewModel<RandomPhotoState, RandomPhotoIntent, RandomPhotoEffect>(RandomPhotoState()) {
    private var fetchJob: Job? = null

    init {
        dispatch(RandomPhotoIntent.FetchRandomPhoto())
    }

    override fun dispatch(intent: RandomPhotoIntent) {
        when (intent) {
            is RandomPhotoIntent.FetchRandomPhoto -> fetchRandomPhoto(intent.query, intent.orientation)
            is RandomPhotoIntent.TrackDownload -> trackDownload()
        }
    }

    private fun fetchRandomPhoto(
        query: String? = null,
        orientation: String? = null,
    ) {
        fetchJob?.cancel()
        updateState { it.copy(isLoading = true, error = null) }

        fetchJob =
            viewModelScope.launch {
                try {
                    val photos = repository.getRandomPhotos(orientation = orientation, query = query, count = 1)
                    val photo = photos.firstOrNull()
                    updateState {
                        it.copy(
                            photo = photo,
                            isLoading = false,
                            error = null,
                        )
                    }
                    if (photo != null) {
                        sendEffect(RandomPhotoEffect.PhotoFetched(photo.id))
                    } else {
                        val message = "No random photo returned"
                        updateState { it.copy(error = message) }
                        sendEffect(RandomPhotoEffect.FetchFailed(message))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load random photo"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = message,
                        )
                    }
                    sendEffect(RandomPhotoEffect.FetchFailed(message))
                }
            }
    }

    private fun trackDownload() {
        state.value.photo?.let { photo ->
            viewModelScope.launch {
                repository.trackDownload(photo.links.downloadLocation)
            }
        }
    }

    override fun teardown() {
        super.teardown()
        fetchJob?.cancel()
    }
}
