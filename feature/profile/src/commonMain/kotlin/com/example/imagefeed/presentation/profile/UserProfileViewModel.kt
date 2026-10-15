package com.example.imagefeed.presentation.profile

import androidx.lifecycle.viewModelScope
import com.example.imagefeed.core.network.PhotoDownloadTracker
import com.example.imagefeed.core.network.filterDistinctAgainst
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.mvi.MviViewModel
import com.example.imagefeed.profile.data.ProfileRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AssistedInject
class UserProfileViewModel(
    @Assisted private val username: String,
    private val repository: ProfileRepository,
    private val downloadTracker: PhotoDownloadTracker,
) : MviViewModel<UserProfileState, UserProfileIntent, UserProfileEffect>(UserProfileState()) {
    @AssistedFactory
    interface Factory {
        fun create(username: String): UserProfileViewModel
    }

    private var profileJob: Job? = null
    private var contentJob: Job? = null
    private var statsJob: Job? = null

    init {
        dispatch(UserProfileIntent.LoadProfile)
    }

    override fun dispatch(intent: UserProfileIntent) {
        when (intent) {
            is UserProfileIntent.LoadProfile -> loadProfile()
            is UserProfileIntent.SelectTab -> selectTab(intent.tab)
            is UserProfileIntent.LoadNextPage -> loadNextPage()
            is UserProfileIntent.LoadNextPhotosPage -> loadNextPhotosPage()
            is UserProfileIntent.LoadNextLikesPage -> loadNextLikesPage()
            is UserProfileIntent.LoadNextCollectionsPage -> loadNextCollectionsPage()
            is UserProfileIntent.Refresh -> refresh()
            is UserProfileIntent.TrackDownload -> trackDownload(intent.photo)
        }
    }

    private fun loadProfile() {
        profileJob?.cancel()
        updateState { it.copy(isHeaderLoading = true, error = null) }
        profileJob =
            viewModelScope.launch {
                try {
                    val profile = repository.getUserProfile(username)
                    updateState { it.copy(user = profile, isHeaderLoading = false) }
                    loadNextPage()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load user profile"
                    updateState {
                        it.copy(
                            isHeaderLoading = false,
                            error = message,
                        )
                    }
                    sendEffect(UserProfileEffect.ShowError(message))
                }
            }
    }

    private fun selectTab(tab: ProfileTab) {
        val current = state.value
        if (current.activeTab == tab) return

        updateState { it.copy(activeTab = tab, error = null) }

        when (tab) {
            ProfileTab.PORTFOLIO -> {
                if (current.portfolioPhotos.isEmpty() && !current.portfolioReachedEnd) {
                    loadNextPhotosPage()
                }
            }
            ProfileTab.LIKES -> {
                if (current.likedPhotos.isEmpty() && !current.likesReachedEnd) {
                    loadNextLikesPage()
                }
            }
            ProfileTab.COLLECTIONS -> {
                if (current.collections.isEmpty() && !current.collectionsReachedEnd) {
                    loadNextCollectionsPage()
                }
            }
            ProfileTab.INSIGHTS -> {
                if (current.stats == null) {
                    loadStats()
                }
            }
        }
    }

    private fun loadStats() {
        val currentState = state.value
        if (currentState.isLoadingStats) return

        statsJob?.cancel()
        updateState { it.copy(isLoadingStats = true, error = null) }
        statsJob =
            viewModelScope.launch {
                try {
                    val userStats = repository.getUserStats(username)
                    updateState {
                        it.copy(
                            stats = userStats,
                            isLoadingStats = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load stats"
                    updateState {
                        it.copy(
                            isLoadingStats = false,
                            error = message,
                        )
                    }
                    sendEffect(UserProfileEffect.ShowError(message))
                }
            }
    }

    private fun loadNextPage() {
        when (state.value.activeTab) {
            ProfileTab.PORTFOLIO -> loadNextPhotosPage()
            ProfileTab.LIKES -> loadNextLikesPage()
            ProfileTab.COLLECTIONS -> loadNextCollectionsPage()
            ProfileTab.INSIGHTS -> Unit
        }
    }

    private fun loadNextPhotosPage() {
        val currentState = state.value
        if (currentState.isLoadingContent || currentState.portfolioReachedEnd) return

        contentJob?.cancel()
        updateState { it.copy(isLoadingContent = true, error = null) }
        contentJob =
            viewModelScope.launch {
                try {
                    val nextPage = if (currentState.portfolioPhotos.isEmpty()) 1 else currentState.portfolioPage + 1
                    val items = repository.getUserPhotos(username, page = nextPage, perPage = 15)

                    updateState { current ->
                        val uniqueNew = items.filterDistinctAgainst(current.portfolioPhotos) { photo -> photo.id }
                        current.copy(
                            portfolioPhotos = current.portfolioPhotos + uniqueNew,
                            portfolioPage = nextPage,
                            portfolioReachedEnd = items.size < 15 || (items.isNotEmpty() && uniqueNew.isEmpty()),
                            isLoadingContent = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load uploaded photos"
                    updateState {
                        it.copy(
                            isLoadingContent = false,
                            error = message,
                        )
                    }
                    sendEffect(UserProfileEffect.ShowError(message))
                }
            }
    }

    private fun loadNextLikesPage() {
        val currentState = state.value
        if (currentState.isLoadingContent || currentState.likesReachedEnd) return

        contentJob?.cancel()
        updateState { it.copy(isLoadingContent = true, error = null) }
        contentJob =
            viewModelScope.launch {
                try {
                    val nextPage = if (currentState.likedPhotos.isEmpty()) 1 else currentState.likesPage + 1
                    val items = repository.getUserLikes(username, page = nextPage, perPage = 15)

                    updateState { current ->
                        val uniqueNew = items.filterDistinctAgainst(current.likedPhotos) { photo -> photo.id }
                        current.copy(
                            likedPhotos = current.likedPhotos + uniqueNew,
                            likesPage = nextPage,
                            likesReachedEnd = items.size < 15 || (items.isNotEmpty() && uniqueNew.isEmpty()),
                            isLoadingContent = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load liked photos"
                    updateState {
                        it.copy(
                            isLoadingContent = false,
                            error = message,
                        )
                    }
                    sendEffect(UserProfileEffect.ShowError(message))
                }
            }
    }

    private fun loadNextCollectionsPage() {
        val currentState = state.value
        if (currentState.isLoadingContent || currentState.collectionsReachedEnd) return

        contentJob?.cancel()
        updateState { it.copy(isLoadingContent = true, error = null) }
        contentJob =
            viewModelScope.launch {
                try {
                    val nextPage = if (currentState.collections.isEmpty()) 1 else currentState.collectionsPage + 1
                    val items = repository.getUserCollections(username, page = nextPage, perPage = 15)

                    updateState { current ->
                        val uniqueNew = items.filterDistinctAgainst(current.collections) { col -> col.id }
                        current.copy(
                            collections = current.collections + uniqueNew,
                            collectionsPage = nextPage,
                            collectionsReachedEnd = items.size < 15 || (items.isNotEmpty() && uniqueNew.isEmpty()),
                            isLoadingContent = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "Failed to load user collections"
                    updateState {
                        it.copy(
                            isLoadingContent = false,
                            error = message,
                        )
                    }
                    sendEffect(UserProfileEffect.ShowError(message))
                }
            }
    }

    private fun refresh() {
        updateState {
            it.copy(
                portfolioPhotos = emptyList(),
                likedPhotos = emptyList(),
                collections = emptyList(),
                stats = null,
                portfolioPage = 1,
                portfolioReachedEnd = false,
                likesPage = 1,
                likesReachedEnd = false,
                collectionsPage = 1,
                collectionsReachedEnd = false,
                error = null,
            )
        }
        loadProfile()
    }

    private fun trackDownload(photo: Photo) {
        viewModelScope.launch {
            downloadTracker.trackDownload(photo.links.downloadLocation)
        }
    }

    override fun teardown() {
        super.teardown()
        profileJob?.cancel()
        contentJob?.cancel()
        statsJob?.cancel()
    }
}
