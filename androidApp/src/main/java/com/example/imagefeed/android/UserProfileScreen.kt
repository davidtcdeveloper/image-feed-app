package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import com.example.imagefeed.android.components.PhotoCard
import com.example.imagefeed.android.profile.CollectionRowLayout
import com.example.imagefeed.android.profile.InsightsTabContent
import com.example.imagefeed.android.profile.ProfileHeaderSection
import com.example.imagefeed.android.profile.ProfileTabSelector
import com.example.imagefeed.android.theme.ExpressiveWavyProgressIndicator
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.PhotoCardSkeleton
import com.example.imagefeed.android.util.UserProfileHeaderSkeleton
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.profile.ProfileTab
import com.example.imagefeed.presentation.profile.UserProfileEffect
import com.example.imagefeed.presentation.profile.UserProfileIntent
import com.example.imagefeed.presentation.profile.UserProfileState
import com.example.imagefeed.presentation.profile.UserProfileViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserProfileScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    username: String,
    viewModel: UserProfileViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(key = "user_$username") {
            MetroHelper.getUserProfileViewModel(username)
        },
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onCollectionClick: (PhotoCollection) -> Unit,
    onUserClick: (User) -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = UserProfileState())

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UserProfileEffect.NavigateBack -> onBack()
                is UserProfileEffect.ShowError -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.user?.name ?: stringResource(R.string.profile_title_fallback),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                        )
                    }
                },
                actions = {
                    state.user?.let { user ->
                        IconButton(onClick = {
                            try {
                                val profileUrl = "${user.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl))
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {
                                // Ignore if browser is not available
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.profile_action_open_browser_accessibility),
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            if (state.isHeaderLoading && state.user == null) {
                UserProfileHeaderSkeleton()
            } else if (state.error != null && state.user == null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.profile_error_loading),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error?.localizedMessage() ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.dispatch(UserProfileIntent.LoadProfile) },
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            } else {
                state.user?.let { user ->
                    UserProfileContent(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        user = user,
                        state = state,
                        onTabSelect = { viewModel.dispatch(UserProfileIntent.SelectTab(it)) },
                        onLoadMore = { viewModel.dispatch(UserProfileIntent.LoadNextPage) },
                        onPhotoClick = onPhotoClick,
                        onCollectionClick = onCollectionClick,
                        onUserClick = onUserClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserProfileContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    user: User,
    state: UserProfileState,
    onTabSelect: (ProfileTab) -> Unit,
    onLoadMore: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onCollectionClick: (PhotoCollection) -> Unit,
    onUserClick: (User) -> Unit = {},
) {
    val gridState = rememberLazyStaggeredGridState()
    val currentLoading by rememberUpdatedState(state.isLoadingContent)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentHasReachedEnd by rememberUpdatedState(
        when (state.activeTab) {
            ProfileTab.PORTFOLIO -> state.portfolioReachedEnd
            ProfileTab.LIKES -> state.likesReachedEnd
            ProfileTab.COLLECTIONS -> state.collectionsReachedEnd
            ProfileTab.INSIGHTS -> true
        },
    )

    LaunchedEffect(gridState, state.activeTab) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 2 && lastVisible >= totalItems - 6
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd && state.activeTab != ProfileTab.INSIGHTS }
            .collect {
                currentOnLoadMore()
            }
    }

    var selectedPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    val itemWidthPx = calculatePhotoItemWidthPx()
    val skeletonAspectRatios = remember { listOf(0.7f, 1.2f, 1.5f, 0.8f, 1.0f, 1.3f) }

    LazyVerticalStaggeredGrid(
        columns = if (state.activeTab == ProfileTab.COLLECTIONS) calculateCollectionGridColumns() else calculateGridColumns(),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = calculateBottomContentPadding(8.dp),
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        // Sticky Profile Info Card
        item(span = StaggeredGridItemSpan.FullLine, key = "profile_header", contentType = "header") {
            ProfileHeaderSection(user = user)
        }

        // Custom Navigation Tab Row
        item(span = StaggeredGridItemSpan.FullLine, key = "profile_tabs", contentType = "tabs") {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                ProfileTabSelector(
                    activeTab = state.activeTab,
                    user = user,
                    onTabSelect = onTabSelect,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        when (state.activeTab) {
            ProfileTab.PORTFOLIO, ProfileTab.LIKES -> {
                val photos = if (state.activeTab == ProfileTab.PORTFOLIO) state.portfolioPhotos else state.likedPhotos
                val showAttribution = state.activeTab == ProfileTab.LIKES

                if (photos.isEmpty() && state.isLoadingContent) {
                    items(12, key = { "photo_skeleton_$it" }, contentType = { "photo_skeleton" }) { index ->
                        PhotoCardSkeleton(aspectRatio = skeletonAspectRatios[index % skeletonAspectRatios.size])
                    }
                } else if (photos.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "empty_${state.activeTab}", contentType = "empty") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_empty_photos),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        photos,
                        key = { _, photo -> "${state.activeTab}_${photo.id}" },
                        contentType = { _, _ -> "photo_card" },
                    ) { index, photo ->
                        PhotoCard(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            index = index,
                            photo = photo,
                            itemWidthPx = itemWidthPx,
                            isTransitionSource = photo.id == selectedPhotoId,
                            onClick = {
                                selectedPhotoId = photo.id
                                onPhotoClick(photo)
                            },
                            onUserClick = { onUserClick(photo.user) },
                            showAttribution = showAttribution,
                        )
                    }

                    if (state.isLoadingContent) {
                        item(span = StaggeredGridItemSpan.FullLine, contentType = "loading_indicator") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                ExpressiveWavyProgressIndicator()
                            }
                        }
                    }
                }
            }
            ProfileTab.COLLECTIONS -> {
                val collections = state.collections

                if (collections.isEmpty() && state.isLoadingContent) {
                    items(3, key = { "col_skeleton_$it" }, contentType = { "col_skeleton" }) {
                        CollectionMosaicCardSkeleton()
                    }
                } else if (collections.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "empty_collections", contentType = "empty") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_empty_collections),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        collections,
                        key = { _, col -> "col_${col.id}" },
                        contentType = { _, _ -> "collection_card" },
                    ) { index, col ->
                        CollectionRowLayout(
                            collection = col,
                            modifier = Modifier.staggeredEntrance(index),
                            onClick = { onCollectionClick(col) },
                        )
                    }

                    if (state.isLoadingContent) {
                        item(span = StaggeredGridItemSpan.FullLine, contentType = "loading_indicator") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                ExpressiveWavyProgressIndicator()
                            }
                        }
                    }
                }
            }
            ProfileTab.INSIGHTS -> {
                item(span = StaggeredGridItemSpan.FullLine, key = "insights_content", contentType = "insights") {
                    InsightsTabContent(
                        stats = state.stats,
                        isLoading = state.isLoadingStats,
                        error = state.error,
                    )
                }
            }
        }
    }
}
