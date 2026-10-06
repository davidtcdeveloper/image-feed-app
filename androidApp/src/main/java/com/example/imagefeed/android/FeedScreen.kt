package com.example.imagefeed.android

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.FeedPresenter
import com.example.imagefeed.presentation.FeedState
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    presenter: FeedPresenter,
    reselectEvents: SharedFlow<NavKey>? = null,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onSearchClick: () -> Unit,
    onRandomClick: () -> Unit,
) {
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = FeedState())
    val listState = rememberLazyStaggeredGridState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key == AppRoute.Feed) {
                listState.animateScrollToItem(0)
            }
        }
    }

    LaunchedEffect(pullToRefreshState.distanceFraction) {
        if (pullToRefreshState.distanceFraction >= 1f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Infinite scrolling logic
    val shouldLoadMore =
        remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItems = listState.layoutInfo.totalItemsCount
                if (lastVisibleItem == null || totalItems == 0) {
                    false
                } else {
                    lastVisibleItem.index >= totalItems - 6
                }
            }
        }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            presenter.loadNextPage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "FEED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                            )
                        }
                        IconButton(onClick = onRandomClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Randomize",
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

                val activeIndex =
                    if (state.selectedTopicSlug == "editorial") {
                        0
                    } else {
                        val idx = state.topics.indexOfFirst { it.slug == state.selectedTopicSlug }
                        if (idx >= 0) idx + 1 else 0
                    }

                SecondaryScrollableTabRow(
                    selectedTabIndex = activeIndex,
                    containerColor =
                        if (scrollBehavior.state.contentOffset < 0f) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 12.dp,
                ) {
                    Tab(
                        selected = state.selectedTopicSlug == "editorial",
                        onClick = { presenter.selectTopic("editorial") },
                        text = {
                            Text(
                                "Editorial",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight =
                                    if (state.selectedTopicSlug == "editorial") {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                            )
                        },
                    )
                    state.topics.forEach { topic ->
                        val isSelected = state.selectedTopicSlug == topic.slug
                        Tab(
                            selected = isSelected,
                            onClick = { presenter.selectTopic(topic.slug) },
                            text = {
                                Text(
                                    topic.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                presenter.refresh()
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
                ErrorView(error = state.error ?: "Unknown error", onRetry = { presenter.refresh() })
            } else {
                // Main content: Photo Feed
                LazyVerticalStaggeredGrid(
                    columns = calculateGridColumns(),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    itemsIndexed(state.photos, key = { _, photo -> photo.id }) { index, photo ->
                        PhotoCard(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            index = index,
                            photo = photo,
                            onClick = {
                                onPhotoClick(photo)
                            },
                            onUserClick = {
                                onUserClick(photo.user)
                            },
                        )
                    }

                    if (state.isLoading) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoCard(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    index: Int,
    photo: Photo,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val itemWidthPx = calculatePhotoItemWidthPx()
    val aspectRatio = photo.width.toFloat() / photo.height.toFloat()

    // Decode BlurHash placeholder in background
    val placeholderBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, photo.blurHash) {
        value = BlurHashDecoder.decode(photo.blurHash, 32, (32 / aspectRatio).toInt())
    }

    val painter =
        rememberAsyncImagePainter(
            model =
                ImageRequest
                    .Builder(context)
                    .data(photo.urls.raw + "&w=" + itemWidthPx + "&q=80&auto=format")
                    .crossfade(true)
                    .build(),
            placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
        )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            Modifier
                .fillMaxWidth()
                .staggeredEntrance(index = index)
                .bounceClick(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            with(sharedTransitionScope) {
                Image(
                    painter = painter,
                    contentDescription = photo.altDescription ?: photo.description,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                    contentScale = ContentScale.Crop,
                )
            }

            // Dynamic bottom overlay containing photographer attribution
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            ),
                        ).padding(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick() },
                ) {
                    val userProfileImagePainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.small)
                                    .crossfade(true)
                                    .build(),
                        )

                    Image(
                        painter = userProfileImagePainter,
                        contentDescription = "User profile",
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = photo.user.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
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
            text = "Error Loading Feed",
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
            Text("Retry")
        }
    }
}
