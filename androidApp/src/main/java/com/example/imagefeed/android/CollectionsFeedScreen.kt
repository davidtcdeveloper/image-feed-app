package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.presentation.collections.CollectionsFeedEffect
import com.example.imagefeed.presentation.collections.CollectionsFeedIntent
import com.example.imagefeed.presentation.collections.CollectionsFeedState
import com.example.imagefeed.presentation.collections.CollectionsFeedViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsFeedScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CollectionsFeedViewModel,
    reselectEvents: kotlinx.coroutines.flow.SharedFlow<androidx.navigation3.runtime.NavKey>? = null,
    onCollectionClick: (PhotoCollection) -> Unit,
    onSearchClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = CollectionsFeedState())
    val listState = rememberLazyStaggeredGridState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CollectionsFeedEffect.ScrollToTop -> listState.animateScrollToItem(0)
                is CollectionsFeedEffect.ShowError -> Unit
            }
        }
    }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key == com.example.imagefeed.android.navigation.AppRoute.Collections) {
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
    val currentLoading by rememberUpdatedState(state.isLoading)
    val currentHasReachedEnd by rememberUpdatedState(state.hasReachedEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                viewModel.dispatch(CollectionsFeedIntent.LoadNextPage)
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.collections_title),
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.nav_search_accessibility),
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
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.dispatch(CollectionsFeedIntent.Refresh)
            },
            state = pullToRefreshState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(paddingValues),
        ) {
            if (state.collections.isEmpty() && state.isLoading) {
                LazyVerticalStaggeredGrid(
                    columns = calculateCollectionGridColumns(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            top = 12.dp,
                            end = 12.dp,
                            bottom = calculateBottomContentPadding(12.dp),
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 16.dp,
                ) {
                    items(6, contentType = { "collection_skeleton" }) {
                        CollectionMosaicCardSkeleton()
                    }
                }
            } else if (state.collections.isEmpty() && state.error != null) {
                ErrorView(error = state.error!!.localizedMessage(), onRetry = { viewModel.dispatch(CollectionsFeedIntent.Refresh) })
            } else {
                val cardWidthPx = calculateCollectionMosaicCardWidthPx()
                val sideWidthPx = remember(cardWidthPx) { cardWidthPx / 3 }
                // UI-element transition state: gates Modifier.sharedBounds so idle lazy items avoid tracking overhead during flings.
                // Does not represent business selection; ViewModel remains the SSOT for collections data.
                var selectedCollectionId by rememberSaveable { mutableStateOf<String?>(null) }

                LazyVerticalStaggeredGrid(
                    columns = calculateCollectionGridColumns(),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            top = 12.dp,
                            end = 12.dp,
                            bottom = calculateBottomContentPadding(12.dp),
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 16.dp,
                ) {
                    itemsIndexed(
                        state.collections,
                        key = { _, collection -> collection.id },
                        contentType = { _, _ -> "collection_card" },
                    ) { index, collection ->
                        CollectionMosaicCard(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            index = index,
                            collection = collection,
                            cardWidthPx = cardWidthPx,
                            sideWidthPx = sideWidthPx,
                            isTransitionSource = collection.id == selectedCollectionId,
                            onClick = {
                                selectedCollectionId = collection.id
                                onCollectionClick(collection)
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
fun CollectionMosaicCard(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    index: Int,
    collection: PhotoCollection,
    onClick: () -> Unit,
    cardWidthPx: Int = calculateCollectionMosaicCardWidthPx(),
    sideWidthPx: Int = cardWidthPx / 3,
    isTransitionSource: Boolean = false,
) {
    val context = LocalPlatformContext.current

    // Use BlurHash from cover photo as card background placeholder with LRU cache
    val blurHash = collection.coverPhoto?.blurHash
    val placeholderBitmap = rememberBlurHashPlaceholder(blurHash, BLURHASH_PLACEHOLDER_WIDTH, 18)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .staggeredEntrance(index = index)
                .bounceClick(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mosaic Grid: Left large cover, right two smaller thumbnails
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
            ) {
                // Left Image: Cover Photo (weight 0.62f)
                val coverUrl =
                    remember(collection.id, collection.coverPhoto?.id, collection.coverPhoto?.urls?.raw, cardWidthPx) {
                        collection.coverPhoto
                            ?.urls
                            ?.raw
                            ?.let { "$it&w=${cardWidthPx * 2 / 3}&q=80&auto=format" }
                            ?: collection.previewPhotos
                                ?.firstOrNull()
                                ?.urls
                                ?.small
                    }

                val coverRequest =
                    remember(context, coverUrl) {
                        coverUrl?.let {
                            ImageRequest
                                .Builder(context)
                                .data(it)
                                .crossfade(true)
                                .build()
                        }
                    }

                val coverPainter =
                    rememberAsyncImagePainter(
                        model = coverRequest,
                        placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
                    )

                Box(
                    modifier =
                        Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    if (coverUrl != null) {
                        with(sharedTransitionScope) {
                            val coverModifier =
                                Modifier
                                    .fillMaxSize()
                                    .let { mod ->
                                        if (isTransitionSource) {
                                            mod.sharedBounds(
                                                sharedContentState =
                                                    rememberSharedContentState(
                                                        key = "col_cover_${collection.id}",
                                                    ),
                                                animatedVisibilityScope = animatedVisibilityScope,
                                            )
                                        } else {
                                            mod
                                        }
                                    }

                            Image(
                                painter = coverPainter,
                                contentDescription = collection.title,
                                modifier = coverModifier,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Column: Two thumbnails (weight 0.38f)
                Column(
                    modifier =
                        Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                ) {
                    // Top Thumbnail
                    val topThumbPhoto =
                        collection.previewPhotos
                            ?.getOrNull(1)
                            ?: collection.previewPhotos
                                ?.getOrNull(0)
                    val topThumbUrl =
                        remember(topThumbPhoto?.id, topThumbPhoto?.urls?.small, sideWidthPx) {
                            topThumbPhoto?.urls?.small?.let { "$it&w=$sideWidthPx&q=80&auto=format" }
                        }
                    val topPlaceholderBitmap =
                        rememberBlurHashPlaceholder(topThumbPhoto?.blurHash, BLURHASH_PLACEHOLDER_WIDTH / 2, 12)

                    val topThumbRequest =
                        remember(context, topThumbUrl) {
                            topThumbUrl?.let {
                                ImageRequest
                                    .Builder(context)
                                    .data(it)
                                    .crossfade(true)
                                    .build()
                            }
                        }

                    val topThumbPainter =
                        rememberAsyncImagePainter(
                            model = topThumbRequest,
                            placeholder = topPlaceholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
                        )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        if (topThumbUrl != null) {
                            Image(
                                painter = topThumbPainter,
                                contentDescription = stringResource(R.string.collections_preview_1_accessibility),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Thumbnail
                    val bottomThumbPhoto =
                        collection.previewPhotos
                            ?.getOrNull(2)
                            ?: collection.previewPhotos
                                ?.getOrNull(1)
                    val bottomThumbUrl =
                        remember(bottomThumbPhoto?.id, bottomThumbPhoto?.urls?.small, sideWidthPx) {
                            bottomThumbPhoto?.urls?.small?.let { "$it&w=$sideWidthPx&q=80&auto=format" }
                        }
                    val bottomPlaceholderBitmap =
                        rememberBlurHashPlaceholder(bottomThumbPhoto?.blurHash, BLURHASH_PLACEHOLDER_WIDTH / 2, 12)

                    val bottomThumbRequest =
                        remember(context, bottomThumbUrl) {
                            bottomThumbUrl?.let {
                                ImageRequest
                                    .Builder(context)
                                    .data(it)
                                    .crossfade(true)
                                    .build()
                            }
                        }

                    val bottomThumbPainter =
                        rememberAsyncImagePainter(
                            model = bottomThumbRequest,
                            placeholder = bottomPlaceholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
                        )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        if (bottomThumbUrl != null) {
                            Image(
                                painter = bottomThumbPainter,
                                contentDescription = stringResource(R.string.collections_preview_2_accessibility),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            // Collection Information Footer
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val description = collection.description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Curator Profile & Attribution
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable {
                                    val userHtml = collection.user.links.html
                                    val utmProfile = "$userHtml?utm_source=ImageFeedApp&utm_medium=referral"
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(utmProfile))
                                    context.startActivity(browserIntent)
                                },
                    ) {
                        val curatorAvatarRequest =
                            remember(context, collection.user.profileImage.small) {
                                ImageRequest
                                    .Builder(context)
                                    .data(collection.user.profileImage.small)
                                    .crossfade(true)
                                    .build()
                            }

                        val avatarPainter =
                            rememberAsyncImagePainter(
                                model = curatorAvatarRequest,
                            )

                        Image(
                            painter = avatarPainter,
                            contentDescription = stringResource(R.string.collections_curator_avatar_accessibility),
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.collections_curated_by),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = collection.user.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Total photos badge
                    Box(
                        modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.collections_photos_count, collection.totalPhotos, collection.totalPhotos),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
