package com.example.imagefeed.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imagefeed.android.components.CollectionMosaicCard
import com.example.imagefeed.android.designsystem.SatinHeaderScaffold
import com.example.imagefeed.android.designsystem.SatinPullToRefreshBox
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.android.theme.ExpressiveWavyProgressIndicator
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.collections.CollectionsFeedEffect
import com.example.imagefeed.presentation.collections.CollectionsFeedIntent
import com.example.imagefeed.presentation.collections.CollectionsFeedState
import com.example.imagefeed.presentation.collections.CollectionsFeedViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsFeedScreen(
    viewModel: CollectionsFeedViewModel,
    reselectEvents: kotlinx.coroutines.flow.SharedFlow<androidx.navigation3.runtime.NavKey>? = null,
    onCollectionClick: (PhotoCollection) -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: (User) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = CollectionsFeedState())
    val listState = rememberLazyStaggeredGridState()

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

    CollectionsFeedScreenContent(
        state = state,
        listState = listState,
        onRefresh = { viewModel.dispatch(CollectionsFeedIntent.Refresh) },
        onLoadNextPage = { viewModel.dispatch(CollectionsFeedIntent.LoadNextPage) },
        onCollectionClick = onCollectionClick,
        onSearchClick = onSearchClick,
        onUserClick = onUserClick,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsFeedScreenContent(
    state: CollectionsFeedState,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onCollectionClick: (PhotoCollection) -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: (User) -> Unit = {},
    listState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
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
            totalItems > 0 && lastVisible >= totalItems - 4
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                currentOnLoadNextPage()
            }
    }

    SatinHeaderScaffold(
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
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        SatinPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRefresh()
            },
            state = pullToRefreshState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
        ) {
            if (state.collections.isEmpty() && state.isLoading) {
                LazyVerticalStaggeredGrid(
                    columns = calculateCollectionGridColumns(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            top = paddingValues.calculateTopPadding() + 12.dp,
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
                Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    ErrorView(error = state.error!!.localizedMessage(), onRetry = onRefresh)
                }
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
                            top = paddingValues.calculateTopPadding() + 12.dp,
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
                            index = index,
                            collection = collection,
                            cardWidthPx = cardWidthPx,
                            sideWidthPx = sideWidthPx,
                            isTransitionSource = collection.id == selectedCollectionId,
                            onClick = {
                                selectedCollectionId = collection.id
                                onCollectionClick(collection)
                            },
                            onUserClick = onUserClick,
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

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Collections - Compact Light")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsFeedScreenPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { _, _ ->
        CollectionsFeedScreenContent(
            state = UiPreviewFixtures.collectionsFeedState,
            onRefresh = {},
            onLoadNextPage = {},
            onCollectionClick = {},
            onSearchClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Collections - Compact Dark")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsFeedScreenDarkPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = true) { _, _ ->
        CollectionsFeedScreenContent(
            state = UiPreviewFixtures.collectionsFeedState,
            onRefresh = {},
            onLoadNextPage = {},
            onCollectionClick = {},
            onSearchClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700, name = "Collections - Expanded")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsFeedScreenExpandedPreview() {
    PreviewScaffold(widthDp = 900, heightDp = 700, darkTheme = false) { _, _ ->
        CollectionsFeedScreenContent(
            state = UiPreviewFixtures.collectionsFeedState,
            onRefresh = {},
            onLoadNextPage = {},
            onCollectionClick = {},
            onSearchClick = {},
        )
    }
}
