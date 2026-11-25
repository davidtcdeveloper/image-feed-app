package com.example.imagefeed.android

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.components.PhotoCard
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.android.theme.ExpressiveWavyProgressIndicator
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.feed.FeedEffect
import com.example.imagefeed.presentation.feed.FeedIntent
import com.example.imagefeed.presentation.feed.FeedState
import com.example.imagefeed.presentation.feed.FeedViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: FeedViewModel,
    reselectEvents: SharedFlow<NavKey>? = null,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onRandomClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = FeedState())
    val listState = rememberLazyStaggeredGridState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FeedEffect.ScrollToTop -> listState.animateScrollToItem(0)
                is FeedEffect.ShowToast -> Unit
            }
        }
    }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key == AppRoute.Feed) {
                listState.animateScrollToItem(0)
            }
        }
    }

    FeedScreenContent(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        state = state,
        listState = listState,
        onSelectTopic = { slug -> viewModel.dispatch(FeedIntent.SelectTopic(slug)) },
        onRefresh = { viewModel.dispatch(FeedIntent.Refresh) },
        onLoadNextPage = { viewModel.dispatch(FeedIntent.LoadNextPage) },
        onPhotoClick = onPhotoClick,
        onUserClick = onUserClick,
        onRandomClick = onRandomClick,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreenContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: FeedState,
    onSelectTopic: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onRandomClick: () -> Unit,
    listState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isScrolled by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior.state.contentOffset < 0f }
    }
    val topicStripBackgroundColor by animateColorAsState(
        targetValue =
            if (isScrolled) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        label = "TopicStripBackgroundColor",
    )
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(pullToRefreshState.distanceFraction) {
        if (pullToRefreshState.distanceFraction >= 1f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Infinite scrolling logic
    val currentLoading by rememberUpdatedState(state.isLoading)
    val currentHasReachedEnd by rememberUpdatedState(state.hasReachedEnd)
    val currentOnLoadNextPage by rememberUpdatedState(onLoadNextPage)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                currentOnLoadNextPage()
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.feed_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(onClick = onRandomClick) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = stringResource(R.string.feed_randomize_accessibility),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    scrollBehavior = scrollBehavior,
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(topicStripBackgroundColor)
                            }.padding(vertical = 8.dp),
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "topic_editorial") {
                            TopicCapsuleCard(
                                title = stringResource(R.string.feed_editorial_tab),
                                coverPhoto = null,
                                isSelected = state.selectedTopicSlug == "editorial",
                                onClick = { onSelectTopic("editorial") },
                            )
                        }
                        items(
                            items = state.topics,
                            key = { it.id },
                        ) { topic ->
                            TopicCapsuleCard(
                                title = topic.title,
                                coverPhoto = topic.coverPhoto,
                                isSelected = state.selectedTopicSlug == topic.slug,
                                onClick = { onSelectTopic(topic.slug) },
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRefresh()
            },
            state = pullToRefreshState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (state.photos.isEmpty() && state.isLoading) {
                PhotoGridSkeleton()
            } else if (state.photos.isEmpty() && state.error != null) {
                ErrorView(error = state.error?.localizedMessage() ?: stringResource(R.string.feed_error_unknown), onRetry = onRefresh)
            } else {
                // Main content: Photo Feed
                val itemWidthPx = calculatePhotoItemWidthPx()
                // UI-element transition state: gates Modifier.sharedElement so idle lazy items avoid tracking overhead during flings.
                // Does not represent business selection; ViewModel remains the SSOT for feed data.
                var selectedPhotoId by rememberSaveable { mutableStateOf<String?>(null) }

                LazyVerticalStaggeredGrid(
                    columns = calculateGridColumns(),
                    state = listState,
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
                    itemsIndexed(
                        state.photos,
                        key = { _, photo -> photo.id },
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
                            onUserClick = {
                                onUserClick(photo.user)
                            },
                        )
                    }

                    if (state.isLoading) {
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
        }
    }
}

@Composable
fun ErrorView(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.feed_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(R.string.feed_action_retry))
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun TopicCapsuleCard(
    title: String,
    coverPhoto: Photo?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val blurHash = coverPhoto?.blurHash
    val placeholderPainter = rememberBlurHashPlaceholder(blurHash, 16, 12)
    val coverUrl = coverPhoto?.urls?.small ?: coverPhoto?.urls?.regular
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val labelColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Card(
        shape = MaterialTheme.shapes.large,
        border =
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier =
            modifier
                .width(110.dp)
                .height(60.dp)
                .semantics {
                    role = Role.Tab
                    selected = isSelected
                }.bounceClick(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUrl != null) {
                Image(
                    painter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(coverUrl)
                                    .crossfade(true)
                                    .build(),
                            placeholder = placeholderPainter,
                        ),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        containerColor.copy(alpha = 0.62f),
                                        containerColor.copy(alpha = 0.84f),
                                    ),
                                ),
                            ),
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private const val PAGINATION_THRESHOLD = 6

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Feed - Compact Light")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreenPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { sharedScope, visibilityScope ->
        FeedScreenContent(
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = visibilityScope,
            state = UiPreviewFixtures.feedState,
            onSelectTopic = {},
            onRefresh = {},
            onLoadNextPage = {},
            onPhotoClick = {},
            onUserClick = {},
            onRandomClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Feed - Compact Dark")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreenDarkPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = true) { sharedScope, visibilityScope ->
        FeedScreenContent(
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = visibilityScope,
            state = UiPreviewFixtures.feedState,
            onSelectTopic = {},
            onRefresh = {},
            onLoadNextPage = {},
            onPhotoClick = {},
            onUserClick = {},
            onRandomClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700, name = "Feed - Expanded")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreenExpandedPreview() {
    PreviewScaffold(widthDp = 900, heightDp = 700, darkTheme = false) { sharedScope, visibilityScope ->
        FeedScreenContent(
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = visibilityScope,
            state = UiPreviewFixtures.feedState,
            onSelectTopic = {},
            onRefresh = {},
            onLoadNextPage = {},
            onPhotoClick = {},
            onUserClick = {},
            onRandomClick = {},
        )
    }
}
