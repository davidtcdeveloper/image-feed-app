package com.example.imagefeed.presentation

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserStats
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

enum class ProfileTab { PORTFOLIO, LIKES, COLLECTIONS, INSIGHTS }

data class UserProfileState(
    val user: User? = null,
    val activeTab: ProfileTab = ProfileTab.PORTFOLIO,
    val portfolioPhotos: List<Photo> = emptyList(),
    val likedPhotos: List<Photo> = emptyList(),
    val collections: List<PhotoCollection> = emptyList(),
    val stats: UserStats? = null,
    val isLoadingContent: Boolean = false,
    val isHeaderLoading: Boolean = false,
    val error: String? = null,
    val portfolioPage: Int = 1,
    val portfolioReachedEnd: Boolean = false,
    val likesPage: Int = 1,
    val likesReachedEnd: Boolean = false,
    val collectionsPage: Int = 1,
    val collectionsReachedEnd: Boolean = false,
    val isLoadingStats: Boolean = false,
)

class UserProfilePresenter(
    private val repository: UnsplashRepository,
    private val username: String,
    private val presenterScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()
    val iosState: CommonFlow<UserProfileState> = CommonFlow(state)

    init {
        loadProfile()
    }

    fun loadProfile() {
        _state.update { it.copy(isHeaderLoading = true, error = null) }
        presenterScope.launch {
            try {
                val profile = repository.getUserProfile(username)
                _state.update { it.copy(user = profile, isHeaderLoading = false) }
                // Fetch the default tab's first page of content
                loadNextPage()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isHeaderLoading = false,
                        error = e.message ?: "Failed to load user profile",
                    )
                }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        val current = _state.value
        if (current.activeTab == tab) return

        _state.update { it.copy(activeTab = tab, error = null) }

        // Trigger load for the new tab if it's currently empty and not already loading
        when (tab) {
            ProfileTab.PORTFOLIO -> {
                if (current.portfolioPhotos.isEmpty() && !current.portfolioReachedEnd) {
                    loadNextPage()
                }
            }
            ProfileTab.LIKES -> {
                if (current.likedPhotos.isEmpty() && !current.likesReachedEnd) {
                    loadNextPage()
                }
            }
            ProfileTab.COLLECTIONS -> {
                if (current.collections.isEmpty() && !current.collectionsReachedEnd) {
                    loadNextPage()
                }
            }
            ProfileTab.INSIGHTS -> {
                if (current.stats == null) {
                    loadStats()
                }
            }
        }
    }

    fun loadStats() {
        val currentState = _state.value
        if (currentState.isLoadingStats) return

        _state.update { it.copy(isLoadingStats = true, error = null) }
        presenterScope.launch {
            try {
                val userStats = repository.getUserStats(username)
                _state.update {
                    it.copy(
                        stats = userStats,
                        isLoadingStats = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingStats = false,
                        error = e.message ?: "Failed to load stats",
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val currentState = _state.value
        if (currentState.isLoadingContent) return

        when (currentState.activeTab) {
            ProfileTab.PORTFOLIO -> {
                if (currentState.portfolioReachedEnd) return
                _state.update { it.copy(isLoadingContent = true, error = null) }
                presenterScope.launch {
                    try {
                        val nextPage = if (currentState.portfolioPhotos.isEmpty()) 1 else currentState.portfolioPage + 1
                        val items = repository.getUserPhotos(username, page = nextPage, perPage = 15)
                        _state.update {
                            it.copy(
                                portfolioPhotos = it.portfolioPhotos + items,
                                portfolioPage = nextPage,
                                portfolioReachedEnd = items.size < 15,
                                isLoadingContent = false,
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                isLoadingContent = false,
                                error = e.message ?: "Failed to load uploaded photos",
                            )
                        }
                    }
                }
            }
            ProfileTab.LIKES -> {
                if (currentState.likesReachedEnd) return
                _state.update { it.copy(isLoadingContent = true, error = null) }
                presenterScope.launch {
                    try {
                        val nextPage = if (currentState.likedPhotos.isEmpty()) 1 else currentState.likesPage + 1
                        val items = repository.getUserLikes(username, page = nextPage, perPage = 15)
                        _state.update {
                            it.copy(
                                likedPhotos = it.likedPhotos + items,
                                likesPage = nextPage,
                                likesReachedEnd = items.size < 15,
                                isLoadingContent = false,
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                isLoadingContent = false,
                                error = e.message ?: "Failed to load liked photos",
                            )
                        }
                    }
                }
            }
            ProfileTab.COLLECTIONS -> {
                if (currentState.collectionsReachedEnd) return
                _state.update { it.copy(isLoadingContent = true, error = null) }
                presenterScope.launch {
                    try {
                        val nextPage = if (currentState.collections.isEmpty()) 1 else currentState.collectionsPage + 1
                        val items = repository.getUserCollections(username, page = nextPage, perPage = 15)
                        _state.update {
                            it.copy(
                                collections = it.collections + items,
                                collectionsPage = nextPage,
                                collectionsReachedEnd = items.size < 15,
                                isLoadingContent = false,
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                isLoadingContent = false,
                                error = e.message ?: "Failed to load user collections",
                            )
                        }
                    }
                }
            }
            ProfileTab.INSIGHTS -> {
                // Statistics has no pagination
            }
        }
    }

    fun refresh() {
        _state.update {
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

    fun trackDownload(photo: Photo) {
        presenterScope.launch {
            repository.trackDownload(photo.links.downloadLocation)
        }
    }
}
