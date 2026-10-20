package com.example.imagefeed.android

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.search.SearchFilters
import com.example.imagefeed.presentation.search.SearchState
import com.example.imagefeed.presentation.search.SearchTab
import com.example.imagefeed.presentation.search.UnifiedSearchEffect
import com.example.imagefeed.presentation.search.UnifiedSearchIntent
import com.example.imagefeed.presentation.search.UnifiedSearchViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialQuery: String = "",
    viewModel: UnifiedSearchViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(key = "search_$initialQuery") {
            MetroHelper.getUnifiedSearchViewModel()
        },
    reselectEvents: SharedFlow<NavKey>? = null,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onCollectionClick: (CollectionSummary) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = SearchState())
    var showFiltersSheet by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val isCompact = adaptiveLayoutInfo.isCompactWidth

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UnifiedSearchEffect.DismissKeyboard -> Unit
                is UnifiedSearchEffect.ScrollToTop -> Unit
                is UnifiedSearchEffect.ShowError -> Unit
            }
        }
    }

    LaunchedEffect(pullToRefreshState.distanceFraction) {
        if (pullToRefreshState.distanceFraction >= 1f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = state.isSearchActive,
        onBackCompleted = {
            viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(false))
        },
    )

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            viewModel.dispatch(UnifiedSearchIntent.UpdateQuery(initialQuery))
        }
    }

    val searchInputField = @Composable {
        SearchBarDefaults.InputField(
            query = state.query,
            onQueryChange = { viewModel.dispatch(UnifiedSearchIntent.UpdateQuery(it)) },
            onSearch = { viewModel.dispatch(UnifiedSearchIntent.SubmitSearch(it)) },
            expanded = state.isSearchActive,
            onExpandedChange = { viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(it)) },
            placeholder = {
                Text(
                    stringResource(R.string.search_input_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                if (state.isSearchActive) {
                    IconButton(onClick = { viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(false)) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_action_collapse_accessibility),
                        )
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                        )
                    }
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.dispatch(UnifiedSearchIntent.UpdateQuery("")) }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.search_action_clear_accessibility),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { showFiltersSheet = true }) {
                        Icon(
                            // Standard List serves as an elegant filter icon
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.search_action_filters_accessibility),
                            tint =
                                if (state.filters !=
                                    SearchFilters()
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                if (isCompact) {
                    SearchBar(
                        inputField = searchInputField,
                        expanded = state.isSearchActive,
                        onExpandedChange = { viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(it)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (state.isSearchActive) 0.dp else 12.dp,
                                    vertical = if (state.isSearchActive) 0.dp else 8.dp,
                                ),
                    ) {
                        SearchSuggestionsAndHistory(
                            history = state.searchHistory,
                            onItemClick = { suggestion ->
                                viewModel.dispatch(UnifiedSearchIntent.SubmitSearch(suggestion))
                            },
                            onDeleteClick = { viewModel.dispatch(UnifiedSearchIntent.RemoveHistoryEntry(it)) },
                            onClearAll = { viewModel.dispatch(UnifiedSearchIntent.ClearHistory) },
                        )
                    }
                } else {
                    DockedSearchBar(
                        inputField = searchInputField,
                        expanded = state.isSearchActive,
                        onExpandedChange = { viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(it)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        SearchSuggestionsAndHistory(
                            history = state.searchHistory,
                            onItemClick = { suggestion ->
                                viewModel.dispatch(UnifiedSearchIntent.SubmitSearch(suggestion))
                            },
                            onDeleteClick = { viewModel.dispatch(UnifiedSearchIntent.RemoveHistoryEntry(it)) },
                            onClearAll = { viewModel.dispatch(UnifiedSearchIntent.ClearHistory) },
                        )
                    }
                }

                if (!state.isSearchActive) {
                    SearchColorDial(
                        selectedColor = state.filters.color,
                        onSelectColor = { color ->
                            viewModel.dispatch(UnifiedSearchIntent.SelectColor(color))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Search Category Tabs
                    SecondaryTabRow(
                        selectedTabIndex = state.activeTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(state.activeTab.ordinal),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    ) {
                        SearchTab.entries.forEach { tab ->
                            val tabLabel =
                                when (tab) {
                                    SearchTab.PHOTOS -> stringResource(R.string.search_tab_photos)
                                    SearchTab.COLLECTIONS -> stringResource(R.string.search_tab_collections)
                                    SearchTab.USERS -> stringResource(R.string.search_tab_users)
                                }
                            Tab(
                                selected = state.activeTab == tab,
                                onClick = { viewModel.dispatch(UnifiedSearchIntent.SelectSearchType(tab)) },
                                text = {
                                    Text(
                                        text = tabLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            if (state.query.isBlank()) {
                if (!state.isSearchActive) {
                    SearchSuggestionsAndHistory(
                        history = state.searchHistory,
                        onItemClick = { viewModel.dispatch(UnifiedSearchIntent.SubmitSearch(it)) },
                        onDeleteClick = { viewModel.dispatch(UnifiedSearchIntent.RemoveHistoryEntry(it)) },
                        onClearAll = { viewModel.dispatch(UnifiedSearchIntent.ClearHistory) },
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.dispatch(UnifiedSearchIntent.Refresh)
                    },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.isLoading) {
                        when (state.activeTab) {
                            SearchTab.PHOTOS -> {
                                PhotoGridSkeleton()
                            }
                            SearchTab.COLLECTIONS -> {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(
                                                start = 12.dp,
                                                top = 12.dp,
                                                end = 12.dp,
                                                bottom = calculateBottomContentPadding(12.dp),
                                            ).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    repeat(SKELETON_COLLECTION_COUNT) {
                                        CollectionMosaicCardSkeleton()
                                    }
                                }
                            }
                            SearchTab.USERS -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    } else if (state.error != null) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.search_failed),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error?.localizedMessage() ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        // Display content depending on selected Tab
                        when (state.activeTab) {
                            SearchTab.PHOTOS -> {
                                if (state.photos.isEmpty()) {
                                    NoResultsView()
                                } else {
                                    PhotosResultGrid(
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        photos = state.photos,
                                        isLoadingMore = state.isLoadingMore,
                                        hasReachedEnd = state.hasReachedEnd,
                                        reselectEvents = reselectEvents,
                                        onLoadMore = { viewModel.dispatch(UnifiedSearchIntent.LoadNextPage) },
                                        onPhotoClick = onPhotoClick,
                                    )
                                }
                            }
                            SearchTab.COLLECTIONS -> {
                                if (state.collections.isEmpty()) {
                                    NoResultsView()
                                } else {
                                    CollectionsResultList(
                                        collections = state.collections,
                                        isLoadingMore = state.isLoadingMore,
                                        hasReachedEnd = state.hasReachedEnd,
                                        reselectEvents = reselectEvents,
                                        onLoadMore = { viewModel.dispatch(UnifiedSearchIntent.LoadNextPage) },
                                        onCollectionClick = onCollectionClick,
                                    )
                                }
                            }
                            SearchTab.USERS -> {
                                if (state.users.isEmpty()) {
                                    NoResultsView()
                                } else {
                                    UsersResultList(
                                        users = state.users,
                                        isLoadingMore = state.isLoadingMore,
                                        hasReachedEnd = state.hasReachedEnd,
                                        reselectEvents = reselectEvents,
                                        onLoadMore = { viewModel.dispatch(UnifiedSearchIntent.LoadNextPage) },
                                        onUserClick = onUserClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filters Drawer (Modal Bottom Sheet)
            if (showFiltersSheet) {
                SearchFiltersSheet(
                    filters = state.filters,
                    onDismiss = { showFiltersSheet = false },
                    onApply = { viewModel.dispatch(UnifiedSearchIntent.ApplyFilters(it)) },
                )
            }
        }
    }
}

private val POPULAR_TOPICS =
    listOf(
        "nature",
        "travel",
        "architecture",
        "wallpapers",
        "neon",
        "minimalist",
        "urban",
        "textures",
    )

@Composable
fun SearchSuggestionsAndHistory(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = calculateBottomContentPadding(16.dp),
                ).verticalScroll(rememberScrollState()),
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.search_section_recents),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    stringResource(R.string.search_action_clear_all),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.bounceClick { onClearAll() },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            history.forEach { item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .bounceClick { onItemClick(item) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.content_desc_history),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onDeleteClick(item) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.content_desc_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            stringResource(R.string.search_section_popular_topics),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            POPULAR_TOPICS.forEach { topic ->
                Box(
                    modifier =
                        Modifier
                            .width(120.dp)
                            .height(64.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(suggestionGradient(topic))
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = MaterialTheme.shapes.large,
                            ).bounceClick { onItemClick(topic) }
                            .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = topic.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosResultGrid(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photos: List<Photo>,
    isLoadingMore: Boolean,
    hasReachedEnd: Boolean = false,
    reselectEvents: SharedFlow<NavKey>? = null,
    onLoadMore: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
) {
    val listState = rememberLazyStaggeredGridState()

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key is AppRoute.Search) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val currentLoading by rememberUpdatedState(isLoadingMore)
    val currentHasReachedEnd by rememberUpdatedState(hasReachedEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_PHOTO_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                onLoadMore()
            }
    }

    val itemWidthPx = calculatePhotoItemWidthPx()
    // UI-element transition state: gates Modifier.sharedElement so idle lazy items avoid tracking overhead during flings.
    // Does not represent business selection; ViewModel remains the SSOT for search data.
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
            photos,
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
                    // Handled inside PhotoCard or default link click
                },
            )
        }

        if (isLoadingMore) {
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

@Composable
fun CollectionsResultList(
    collections: List<CollectionSummary>,
    isLoadingMore: Boolean,
    hasReachedEnd: Boolean = false,
    reselectEvents: SharedFlow<NavKey>? = null,
    onLoadMore: () -> Unit,
    onCollectionClick: (CollectionSummary) -> Unit,
) {
    val listState = rememberLazyStaggeredGridState() // staggered grid with 1 column as flexible list

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key is AppRoute.Search) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val currentLoading by rememberUpdatedState(isLoadingMore)
    val currentHasReachedEnd by rememberUpdatedState(hasReachedEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_COLLECTION_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                onLoadMore()
            }
    }

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
            collections,
            key = { _, collection -> collection.id },
            contentType = { _, _ -> "collection_card" },
        ) { index, collection ->
            CollectionRowCard(
                collection = collection,
                modifier = Modifier.staggeredEntrance(index),
                onClick = { onCollectionClick(collection) },
            )
        }

        if (isLoadingMore) {
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

private val CollectionRowScrimBrush =
    Brush.verticalGradient(
        listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.65f),
        ),
    )

@Composable
fun CollectionRowCard(
    collection: CollectionSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val cardWidthPx = calculateCollectionMosaicCardWidthPx()
    val blurHash = collection.coverPhoto?.blurHash
    val placeholderPainter = rememberBlurHashPlaceholder(blurHash, BLURHASH_PLACEHOLDER_WIDTH, 18)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(MaterialTheme.shapes.extraLarge)
                .bounceClick { onClick() },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val coverPhoto = collection.coverPhoto
            val coverUrl =
                remember(coverPhoto?.id, cardWidthPx) {
                    coverPhoto?.urls?.getOptimalUrl(cardWidthPx)
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

            if (coverPhoto != null) {
                Image(
                    painter =
                        rememberAsyncImagePainter(
                            model = coverRequest,
                            placeholder = placeholderPainter,
                        ),
                    contentDescription = collection.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest))
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(CollectionRowScrimBrush)
                        .padding(16.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column {
                    Text(
                        text = collection.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val photosCount =
                        pluralStringResource(R.plurals.collections_photos_count, collection.totalPhotos, collection.totalPhotos)
                    val curatedBy = stringResource(R.string.collections_curated_by)
                    Text(
                        text = "$photosCount  ·  $curatedBy ${collection.user.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
fun UsersResultList(
    users: List<User>,
    isLoadingMore: Boolean,
    hasReachedEnd: Boolean = false,
    reselectEvents: SharedFlow<NavKey>? = null,
    onLoadMore: () -> Unit,
    onUserClick: (User) -> Unit,
) {
    val listState = rememberLazyStaggeredGridState()

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key is AppRoute.Search) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val currentLoading by rememberUpdatedState(isLoadingMore)
    val currentHasReachedEnd by rememberUpdatedState(hasReachedEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_USER_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                onLoadMore()
            }
    }

    LazyVerticalStaggeredGrid(
        columns = calculateUserGridColumns(),
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
        verticalItemSpacing = 12.dp,
    ) {
        itemsIndexed(
            users,
            key = { _, user -> user.id },
            contentType = { _, _ -> "user_card" },
        ) { index, user ->
            UserRowCard(
                user = user,
                modifier = Modifier.staggeredEntrance(index),
                onClick = {
                    onUserClick(user)
                },
            )
        }

        if (isLoadingMore) {
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

@Composable
fun UserRowCard(
    user: User,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val avatarRequest =
        remember(context, user.profileImage.medium) {
            ImageRequest
                .Builder(context)
                .data(user.profileImage.medium)
                .crossfade(true)
                .build()
        }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            modifier
                .fillMaxWidth()
                .bounceClick { onClick() },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = avatarRequest),
                contentDescription = user.name,
                modifier =
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.search_user_card_view_profile_accessibility),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun NoResultsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.search_empty_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    filters: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit,
) {
    var selectedOrderBy by remember { mutableStateOf(filters.orderBy) }
    var selectedOrientation by remember { mutableStateOf(filters.orientation) }
    var selectedColor by remember { mutableStateOf(filters.color) }
    val haptic = LocalHapticFeedback.current

    val anyLabel = stringResource(R.string.search_filter_color_any)
    val bwLabel = stringResource(R.string.search_filter_color_bw)
    val blackLabel = stringResource(R.string.search_filter_color_black)
    val whiteLabel = stringResource(R.string.search_filter_color_white)
    val yellowLabel = stringResource(R.string.search_filter_color_yellow)
    val orangeLabel = stringResource(R.string.search_filter_color_orange)
    val redLabel = stringResource(R.string.search_filter_color_red)
    val purpleLabel = stringResource(R.string.search_filter_color_purple)
    val magentaLabel = stringResource(R.string.search_filter_color_magenta)
    val greenLabel = stringResource(R.string.search_filter_color_green)
    val tealLabel = stringResource(R.string.search_filter_color_teal)
    val blueLabel = stringResource(R.string.search_filter_color_blue)

    val colors =
        remember(
            anyLabel,
            bwLabel,
            blackLabel,
            whiteLabel,
            yellowLabel,
            orangeLabel,
            redLabel,
            purpleLabel,
            magentaLabel,
            greenLabel,
            tealLabel,
            blueLabel,
        ) {
            listOf(
                Pair(anyLabel, null),
                Pair(bwLabel, "black_and_white"),
                Pair(blackLabel, "black"),
                Pair(whiteLabel, "white"),
                Pair(yellowLabel, "yellow"),
                Pair(orangeLabel, "orange"),
                Pair(redLabel, "red"),
                Pair(purpleLabel, "purple"),
                Pair(magentaLabel, "magenta"),
                Pair(greenLabel, "green"),
                Pair(tealLabel, "teal"),
                Pair(blueLabel, "blue"),
            )
        }

    val colorHexes =
        remember {
            mapOf(
                "black_and_white" to Color.Gray,
                "black" to Color.Black,
                "white" to Color.White,
                "yellow" to Color(0xFFFFEB3B),
                "orange" to Color(0xFFFF9800),
                "red" to Color(0xFFF44336),
                "purple" to Color(0xFF9C27B0),
                "magenta" to Color(0xFFE91E63),
                "green" to Color(0xFF4CAF50),
                "teal" to Color(0xFF009688),
                "blue" to Color(0xFF2196F3),
            )
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.search_filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.search_filter_action_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedOrderBy = "relevant"
                            selectedOrientation = null
                            selectedColor = null
                        },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sort Order Section
            Text(
                stringResource(R.string.search_filter_sort_by),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedOrderBy == "relevant",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrderBy = "relevant"
                    },
                    label = { Text(stringResource(R.string.search_filter_sort_relevant)) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedOrderBy == "latest",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrderBy = "latest"
                    },
                    label = { Text(stringResource(R.string.search_filter_sort_latest)) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Orientation Section
            Text(
                stringResource(R.string.search_filter_orientation),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedOrientation == null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrientation = null
                    },
                    label = { Text(stringResource(R.string.search_filter_orientation_all)) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedOrientation == "landscape",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrientation = "landscape"
                    },
                    label = { Text(stringResource(R.string.search_filter_orientation_landscape)) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedOrientation == "portrait",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrientation = "portrait"
                    },
                    label = { Text(stringResource(R.string.search_filter_orientation_portrait)) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Color Section
            Text(
                stringResource(R.string.search_filter_color_tone),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Retained LazyRow over Row+horizontalScroll to maintain nested scroll
            // gesture dispatch with ModalBottomSheet drag handling.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = colors,
                    key = { it.second ?: "any" },
                ) { pair ->
                    val name = pair.first
                    val value = pair.second

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedColor = value
                            },
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorHexes[value] ?: MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .border(
                                        width = if (selectedColor == value) 2.dp else 1.dp,
                                        color =
                                            if (selectedColor == value) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        shape = CircleShape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selectedColor == value) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.content_desc_selected),
                                    tint = if (value == "white" || value == "yellow") Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onApply(
                        SearchFilters(
                            orderBy = selectedOrderBy,
                            color = selectedColor,
                            orientation = selectedOrientation,
                        ),
                    )
                    onDismiss()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    stringResource(R.string.search_filter_action_apply),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private const val SKELETON_COLLECTION_COUNT = 4
private const val PAGINATION_PHOTO_THRESHOLD = 6
private const val PAGINATION_COLLECTION_THRESHOLD = 4
private const val PAGINATION_USER_THRESHOLD = 4

@Suppress("MagicNumber")
private fun suggestionGradient(topic: String): Brush =
    when (topic.lowercase()) {
        "nature" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF1B4332),
                    Color(0xFF2D6A4F),
                    Color(0xFF081C15),
                ),
            )
        "travel" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF1E1B4B),
                    Color(0xFF4338CA),
                    Color(0xFF831843),
                ),
            )
        "architecture" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF1E293B),
                    Color(0xFF334155),
                    Color(0xFF475569),
                ),
            )
        "wallpapers" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF3B0764),
                    Color(0xFF581C87),
                    Color(0xFF701A75),
                ),
            )
        "neon" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF0F172A),
                    Color(0xFF0369A1),
                    Color(0xFF4C1D95),
                ),
            )
        "minimalist" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF18181B),
                    Color(0xFF27272A),
                    Color(0xFF3F3F46),
                ),
            )
        "urban" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF451A03),
                    Color(0xFF78350F),
                    Color(0xFF1C1917),
                ),
            )
        "textures" ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF292524),
                    Color(0xFF44403C),
                    Color(0xFF57534E),
                ),
            )
        else ->
            Brush.linearGradient(
                listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A),
                ),
            )
    }

@Suppress("MagicNumber")
@Composable
private fun SearchColorDial(
    selectedColor: String?,
    onSelectColor: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val colors =
        remember {
            listOf(
                "black_and_white",
                "black",
                "white",
                "yellow",
                "orange",
                "red",
                "purple",
                "magenta",
                "green",
                "teal",
                "blue",
            )
        }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(colors, key = { it }) { colorKey ->
            val isSelected = selectedColor == colorKey
            val ringBorderWidth by animateDpAsState(
                targetValue = if (isSelected) 2.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_ring",
            )
            val ringPadding by animateDpAsState(
                targetValue = if (isSelected) 3.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_padding",
            )
            val ringColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_ring_color",
            )

            val brush =
                remember(colorKey) {
                    when (colorKey) {
                        "black_and_white" ->
                            Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFFF0F0F0)))
                        "black" -> SolidColor(Color.Black)
                        "white" -> SolidColor(Color.White)
                        "yellow" -> SolidColor(Color(0xFFFFEB3B))
                        "orange" -> SolidColor(Color(0xFFFF9800))
                        "red" -> SolidColor(Color(0xFFF44336))
                        "purple" -> SolidColor(Color(0xFF9C27B0))
                        "magenta" -> SolidColor(Color(0xFFE91E63))
                        "green" -> SolidColor(Color(0xFF4CAF50))
                        "teal" -> SolidColor(Color(0xFF009688))
                        "blue" -> SolidColor(Color(0xFF2196F3))
                        else -> SolidColor(Color.Gray)
                    }
                }

            val colorLabel =
                when (colorKey) {
                    "black_and_white" -> stringResource(R.string.search_filter_color_bw)
                    "black" -> stringResource(R.string.search_filter_color_black)
                    "white" -> stringResource(R.string.search_filter_color_white)
                    "yellow" -> stringResource(R.string.search_filter_color_yellow)
                    "orange" -> stringResource(R.string.search_filter_color_orange)
                    "red" -> stringResource(R.string.search_filter_color_red)
                    "purple" -> stringResource(R.string.search_filter_color_purple)
                    "magenta" -> stringResource(R.string.search_filter_color_magenta)
                    "green" -> stringResource(R.string.search_filter_color_green)
                    "teal" -> stringResource(R.string.search_filter_color_teal)
                    "blue" -> stringResource(R.string.search_filter_color_blue)
                    else -> colorKey
                }

            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .semantics {
                            contentDescription = colorLabel
                            role = Role.RadioButton
                            selected = isSelected
                        }.clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val target = if (isSelected) null else colorKey
                            onSelectColor(target)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .border(
                                width = ringBorderWidth,
                                color = ringColor,
                                shape = CircleShape,
                            ).padding(ringPadding)
                            .clip(CircleShape)
                            .then(
                                if (colorKey == "white" && !isSelected) {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                } else {
                                    Modifier
                                },
                            ).background(brush),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (colorKey == "white" || colorKey == "yellow") Color.Black else Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
