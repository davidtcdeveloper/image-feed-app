package com.example.imagefeed.android

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.rememberAsyncImagePainter
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.rememberEntryPresenter
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.SearchFilters
import com.example.imagefeed.presentation.SearchState
import com.example.imagefeed.presentation.SearchTab
import com.example.imagefeed.presentation.UnifiedSearchPresenter
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialQuery: String = "",
    presenter: UnifiedSearchPresenter =
        rememberEntryPresenter(key = "search_$initialQuery", onClear = { it.clear() }) {
            MetroHelper.getUnifiedSearchPresenter()
        },
    reselectEvents: SharedFlow<NavKey>? = null,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onCollectionClick: (CollectionSummary) -> Unit,
) {
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = SearchState())
    var showFiltersSheet by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val isCompact = adaptiveLayoutInfo.isCompactWidth

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
            presenter.setSearchActive(false)
        },
    )

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            presenter.updateQuery(initialQuery)
        }
    }

    val searchInputField = @Composable {
        SearchBarDefaults.InputField(
            query = state.query,
            onQueryChange = { presenter.updateQuery(it) },
            onSearch = { presenter.submitSearch(it) },
            expanded = state.isSearchActive,
            onExpandedChange = { presenter.setSearchActive(it) },
            placeholder = {
                Text(
                    "Search Photos, Collections, Users...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                if (state.isSearchActive) {
                    IconButton(onClick = { presenter.setSearchActive(false) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Collapse Search",
                        )
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { presenter.updateQuery("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { showFiltersSheet = true }) {
                        Icon(
                            // Standard List serves as an elegant filter icon
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Filters",
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
                        onExpandedChange = { presenter.setSearchActive(it) },
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
                            onItemClick = { suggestion -> presenter.submitSearch(suggestion) },
                            onDeleteClick = { presenter.removeHistoryEntry(it) },
                            onClearAll = { presenter.clearHistory() },
                        )
                    }
                } else {
                    DockedSearchBar(
                        inputField = searchInputField,
                        expanded = state.isSearchActive,
                        onExpandedChange = { presenter.setSearchActive(it) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        SearchSuggestionsAndHistory(
                            history = state.searchHistory,
                            onItemClick = { suggestion -> presenter.submitSearch(suggestion) },
                            onDeleteClick = { presenter.removeHistoryEntry(it) },
                            onClearAll = { presenter.clearHistory() },
                        )
                    }
                }

                if (!state.isSearchActive) {
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
                            Tab(
                                selected = state.activeTab == tab,
                                onClick = { presenter.setTab(tab) },
                                text = {
                                    Text(
                                        text = tab.name,
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
                        onItemClick = { presenter.submitSearch(it) },
                        onDeleteClick = { presenter.removeHistoryEntry(it) },
                        onClearAll = { presenter.clearHistory() },
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        presenter.refresh()
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
                                            .padding(12.dp)
                                            .verticalScroll(rememberScrollState()),
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
                                "Search failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error ?: "",
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
                                        reselectEvents = reselectEvents,
                                        onLoadMore = { presenter.loadNextPage() },
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
                                        reselectEvents = reselectEvents,
                                        onLoadMore = { presenter.loadNextPage() },
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
                                        reselectEvents = reselectEvents,
                                        onLoadMore = { presenter.loadNextPage() },
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
                    onApply = { presenter.applyFilters(it) },
                )
            }
        }
    }
}

@Composable
fun SearchSuggestionsAndHistory(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val suggestions =
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

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "RECENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    "CLEAR ALL",
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
                        contentDescription = "History",
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
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            "POPULAR TOPICS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { topic ->
                SuggestionChip(
                    onClick = { onItemClick(topic) },
                    label = {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    colors =
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    border = null,
                )
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
            onLoadMore()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = calculateGridColumns(),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            PhotoCard(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                index = index,
                photo = photo,
                onClick = { onPhotoClick(photo) },
                onUserClick = {
                    // Handled inside PhotoCard or default link click
                },
            )
        }

        if (isLoadingMore) {
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

@Composable
fun CollectionsResultList(
    collections: List<CollectionSummary>,
    isLoadingMore: Boolean,
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

    val shouldLoadMore =
        remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItems = listState.layoutInfo.totalItemsCount
                if (lastVisibleItem == null || totalItems == 0) {
                    false
                } else {
                    lastVisibleItem.index >= totalItems - 4
                }
            }
        }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = calculateCollectionGridColumns(),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 16.dp,
    ) {
        itemsIndexed(collections, key = { _, collection -> collection.id }) { index, collection ->
            CollectionRowCard(
                collection = collection,
                modifier = Modifier.staggeredEntrance(index),
                onClick = { onCollectionClick(collection) },
            )
        }

        if (isLoadingMore) {
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

@Composable
fun CollectionRowCard(
    collection: CollectionSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium)
                .bounceClick { onClick() },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val coverPhoto = collection.coverPhoto
            if (coverPhoto != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = coverPhoto.urls.regular),
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
                        .background(Color.Black.copy(alpha = 0.45f))
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
                    Text(
                        text = "${collection.totalPhotos} Photos  ·  Curated by ${collection.user.name}",
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

    val shouldLoadMore =
        remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItems = listState.layoutInfo.totalItemsCount
                if (lastVisibleItem == null || totalItems == 0) {
                    false
                } else {
                    lastVisibleItem.index >= totalItems - 4
                }
            }
        }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = calculateUserGridColumns(),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
    ) {
        itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
            UserRowCard(
                user = user,
                modifier = Modifier.staggeredEntrance(index),
                onClick = {
                    onUserClick(user)
                },
            )
        }

        if (isLoadingMore) {
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

@Composable
fun UserRowCard(
    user: User,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
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
                painter = rememberAsyncImagePainter(model = user.profileImage.large),
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
                contentDescription = "View Profile",
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
            "No results found.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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

    val colors =
        listOf(
            Pair("Any", null),
            Pair("B&W", "black_and_white"),
            Pair("Black", "black"),
            Pair("White", "white"),
            Pair("Yellow", "yellow"),
            Pair("Orange", "orange"),
            Pair("Red", "red"),
            Pair("Purple", "purple"),
            Pair("Magenta", "magenta"),
            Pair("Green", "green"),
            Pair("Teal", "teal"),
            Pair("Blue", "blue"),
        )

    val colorHexes =
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
                    "FILTERS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "RESET",
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
                "SORT BY",
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
                    label = { Text("RELEVANT") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedOrderBy == "latest",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrderBy = "latest"
                    },
                    label = { Text("LATEST") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Orientation Section
            Text(
                "ORIENTATION",
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
                    label = { Text("ALL") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedOrientation == "landscape",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrientation = "landscape"
                    },
                    label = { Text("LANDSCAPE") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedOrientation == "portrait",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrientation = "portrait"
                    },
                    label = { Text("PORTRAIT") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Color Section
            Text(
                "COLOR TONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(colors) { pair ->
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
                                    contentDescription = "Selected",
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
                Text("APPLY FILTERS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private const val SKELETON_COLLECTION_COUNT = 4
