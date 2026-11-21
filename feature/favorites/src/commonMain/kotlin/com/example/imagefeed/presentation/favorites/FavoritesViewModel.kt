package com.example.imagefeed.presentation.favorites

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.core.database.FavoritesRepository
import com.example.imagefeed.core.network.toAppError
import com.example.imagefeed.presentation.mvi.MviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Inject
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
) : MviViewModel<FavoritesState, FavoritesIntent, FavoritesEffect>(FavoritesState()) {
    private var observeJob: Job? = null

    init {
        observeFavorites()
    }

    override fun dispatch(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.RemoveFavorite -> removeFavorite(intent.photoId)
        }
    }

    private fun observeFavorites() {
        observeJob?.cancel()
        observeJob =
            viewModelScope.launch {
                favoritesRepository
                    .observeFavoritePhotos()
                    .catch { e ->
                        updateState { it.copy(isLoading = false) }
                        sendEffect(FavoritesEffect.ShowErrorSnackbar(e.toAppError()))
                    }.collect { photos ->
                        updateState {
                            it.copy(
                                photos = photos,
                                isLoading = false,
                            )
                        }
                    }
            }
    }

    private fun removeFavorite(photoId: String) {
        viewModelScope.launch {
            try {
                favoritesRepository.removeFavorite(photoId)
                sendEffect(FavoritesEffect.ShowFavoriteRemovedSnackbar)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendEffect(FavoritesEffect.ShowErrorSnackbar(e.toAppError()))
            }
        }
    }

    override fun teardown() {
        super.teardown()
        observeJob?.cancel()
    }
}
