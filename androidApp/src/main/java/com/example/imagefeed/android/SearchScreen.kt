package com.example.imagefeed.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.designsystem.SatinHeaderScaffold
import com.example.imagefeed.android.designsystem.SatinPullToRefreshBox
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.android.search.CollectionsResultList
import com.example.imagefeed.android.search.NoResultsView
import com.example.imagefeed.android.search.PhotosResultGrid
import com.example.imagefeed.android.search.SearchColorDial
import com.example.imagefeed.android.search.SearchFiltersSheet
import com.example.imagefeed.android.search.SearchSuggestionsAndHistory
import com.example.imagefeed.android.search.UsersResultList
import com.example.imagefeed.android.theme.ExpressiveLoadingIndicator
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.localizedMessage
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
import kotlin.math.roundToInt

private const val SKELETON_COLLECTION_COUNT = 4
private val SEARCH_BAR_MAX_WIDTH = 720.dp
internal const val SEARCH_BAR_TAG = "search_bar"
internal const val EXPANDED_SEARCH_BAR_TAG = "expanded_search_bar"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UnifiedSearchEffect.DismissKeyboard -> Unit
                is UnifiedSearchEffect.ScrollToTop -> Unit
                is UnifiedSearchEffect.ShowError -> Unit
            }
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

    SearchScreenContent(
        state = state,
        reselectEvents = reselectEvents,
        onQueryChange = { viewModel.dispatch(UnifiedSearchIntent.UpdateQuery(it)) },
        onSearchSubmit = { viewModel.dispatch(UnifiedSearchIntent.SubmitSearch(it)) },
        onSetSearchActive = { viewModel.dispatch(UnifiedSearchIntent.SetSearchActive(it)) },
        onSelectColor = { viewModel.dispatch(UnifiedSearchIntent.SelectColor(it)) },
        onSelectSearchType = { viewModel.dispatch(UnifiedSearchIntent.SelectSearchType(it)) },
        onApplyFilters = { viewModel.dispatch(UnifiedSearchIntent.ApplyFilters(it)) },
        onRefresh = { viewModel.dispatch(UnifiedSearchIntent.Refresh) },
        onLoadMore = { viewModel.dispatch(UnifiedSearchIntent.LoadNextPage) },
        onRemoveHistoryEntry = { viewModel.dispatch(UnifiedSearchIntent.RemoveHistoryEntry(it)) },
        onClearHistory = { viewModel.dispatch(UnifiedSearchIntent.ClearHistory) },
        onBack = onBack,
        onPhotoClick = onPhotoClick,
        onUserClick = onUserClick,
        onCollectionClick = onCollectionClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreenContent(
    state: SearchState,
    reselectEvents: SharedFlow<NavKey>? = null,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSetSearchActive: (Boolean) -> Unit,
    onSelectColor: (String?) -> Unit,
    onSelectSearchType: (SearchTab) -> Unit,
    onApplyFilters: (SearchFilters) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onCollectionClick: (CollectionSummary) -> Unit,
) {
    var showFiltersSheet by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val isCompact = adaptiveLayoutInfo.isCompactWidth
    val isInspection = LocalInspectionMode.current

    LaunchedEffect(pullToRefreshState.distanceFraction) {
        if (pullToRefreshState.distanceFraction >= 1f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val currentQuery by rememberUpdatedState(state.query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentIsSearchActive by rememberUpdatedState(state.isSearchActive)
    val currentOnSetSearchActive by rememberUpdatedState(onSetSearchActive)

    val textFieldState = rememberTextFieldState(initialText = state.query)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { text ->
                if (text != currentQuery) {
                    currentOnQueryChange(text)
                }
            }
    }
    LaunchedEffect(state.query) {
        if (textFieldState.text.toString() != state.query) {
            textFieldState.setTextAndPlaceCursorAtEnd(state.query)
        }
    }

    val searchBarState =
        rememberSearchBarState(
            initialValue =
                if (state.isSearchActive) {
                    SearchBarValue.Expanded
                } else {
                    SearchBarValue.Collapsed
                },
        )
    if (!isInspection) {
        LaunchedEffect(state.isSearchActive) {
            if (state.isSearchActive && searchBarState.targetValue != SearchBarValue.Expanded) {
                searchBarState.animateToExpanded()
            } else if (!state.isSearchActive && searchBarState.targetValue != SearchBarValue.Collapsed) {
                searchBarState.animateToCollapsed()
            }
        }
        LaunchedEffect(searchBarState) {
            snapshotFlow { searchBarState.targetValue == SearchBarValue.Expanded }
                .distinctUntilChanged()
                .collect { expanded ->
                    if (expanded != currentIsSearchActive) {
                        currentOnSetSearchActive(expanded)
                    }
                }
        }
    }

    val searchInputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = onSearchSubmit,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.search_input_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                if (state.isSearchActive) {
                    IconButton(onClick = { onSetSearchActive(false) }) {
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
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                                onQueryChange("")
                            },
                        ) {
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    SatinHeaderScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(
                modifier = Modifier.windowInsetsPadding(SearchBarDefaults.windowInsets),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SearchBar(
                    state = searchBarState,
                    inputField = searchInputField,
                    modifier =
                        Modifier
                            .padding(
                                horizontal = if (isCompact) 12.dp else 16.dp,
                                vertical = 8.dp,
                            ).widthIn(max = SEARCH_BAR_MAX_WIDTH)
                            .fillMaxWidth()
                            .testTag(SEARCH_BAR_TAG),
                )
                if (isCompact) {
                    ExpandedFullScreenSearchBar(
                        state = searchBarState,
                        inputField = searchInputField,
                        modifier = Modifier.testTag(EXPANDED_SEARCH_BAR_TAG),
                    ) {
                        SearchSuggestionsAndHistory(
                            history = state.searchHistory,
                            onItemClick = onSearchSubmit,
                            onDeleteClick = onRemoveHistoryEntry,
                            onClearAll = onClearHistory,
                        )
                    }
                } else {
                    ExpandedDockedSearchBar(
                        state = searchBarState,
                        inputField = searchInputField,
                        modifier = Modifier.testTag(EXPANDED_SEARCH_BAR_TAG),
                        properties = PopupProperties(focusable = true, blurBehindRadius = 16.dp),
                    ) {
                        SearchSuggestionsAndHistory(
                            history = state.searchHistory,
                            onItemClick = onSearchSubmit,
                            onDeleteClick = onRemoveHistoryEntry,
                            onClearAll = onClearHistory,
                        )
                    }
                }

                if (!state.isSearchActive) {
                    Column(
                        modifier =
                            Modifier
                                .clipToBounds()
                                .layout { measurable, constraints ->
                                    // Quick-return via layout-phase reads (no recomposition).
                                    val placeable = measurable.measure(constraints)
                                    scrollBehavior.state.heightOffsetLimit = -placeable.height.toFloat()
                                    val visibleHeight =
                                        (placeable.height + scrollBehavior.state.heightOffset)
                                            .roundToInt()
                                            .coerceIn(0, placeable.height)
                                    layout(placeable.width, visibleHeight) {
                                        placeable.place(0, visibleHeight - placeable.height)
                                    }
                                },
                    ) {
                        SearchColorDial(
                            selectedColor = state.filters.color,
                            onSelectColor = onSelectColor,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Search Category Tabs
                        SecondaryTabRow(
                            selectedTabIndex = state.activeTab.ordinal,
                            containerColor = Color.Transparent,
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
                                    onClick = { onSelectSearchType(tab) },
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
            }
        },
    ) { paddingValues ->
        val headerTop = paddingValues.calculateTopPadding()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            if (state.query.isBlank()) {
                if (!state.isSearchActive) {
                    Box(modifier = Modifier.padding(top = headerTop)) {
                        SearchSuggestionsAndHistory(
                            history = state.searchHistory,
                            onItemClick = onSearchSubmit,
                            onDeleteClick = onRemoveHistoryEntry,
                            onClearAll = onClearHistory,
                        )
                    }
                }
            } else {
                SatinPullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRefresh()
                    },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.isLoading) {
                        when (state.activeTab) {
                            SearchTab.PHOTOS -> {
                                Box(modifier = Modifier.padding(top = headerTop)) {
                                    PhotoGridSkeleton()
                                }
                            }
                            SearchTab.COLLECTIONS -> {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(
                                                start = 12.dp,
                                                top = headerTop + 12.dp,
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
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(top = headerTop),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ExpressiveLoadingIndicator()
                                }
                            }
                        }
                    } else if (state.error != null) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = headerTop)
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
                                    Box(modifier = Modifier.padding(top = headerTop)) {
                                        NoResultsView()
                                    }
                                } else {
                                    PhotosResultGrid(
                                        photos = state.photos,
                                        isLoadingMore = state.isLoadingMore,
                                        hasReachedEnd = state.hasReachedEnd,
                                        reselectEvents = reselectEvents,
                                        onLoadMore = onLoadMore,
                                        onPhotoClick = onPhotoClick,
                                        onUserClick = onUserClick,
                                    )
                                }
                            }
                            SearchTab.COLLECTIONS -> {
                                if (state.collections.isEmpty()) {
                                    Box(modifier = Modifier.padding(top = headerTop)) {
                                        NoResultsView()
                                    }
                                } else {
                                    CollectionsResultList(
                                        collections = state.collections,
                                        isLoadingMore = state.isLoadingMore,
                                        hasReachedEnd = state.hasReachedEnd,
                                        reselectEvents = reselectEvents,
                                        onLoadMore = onLoadMore,
                                        onCollectionClick = onCollectionClick,
                                    )
                                }
                            }
                            SearchTab.USERS -> {
                                if (state.users.isEmpty()) {
                                    Box(modifier = Modifier.padding(top = headerTop)) {
                                        NoResultsView()
                                    }
                                } else {
                                    UsersResultList(
                                        users = state.users,
                                        isLoadingMore = state.isLoadingMore,
                                        hasReachedEnd = state.hasReachedEnd,
                                        reselectEvents = reselectEvents,
                                        onLoadMore = onLoadMore,
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
                    onApply = onApplyFilters,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Search - Compact Light")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreenPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { _, _ ->
        SearchScreenContent(
            state = UiPreviewFixtures.searchDiscoveryState,
            onQueryChange = {},
            onSearchSubmit = {},
            onSetSearchActive = {},
            onSelectColor = {},
            onSelectSearchType = {},
            onApplyFilters = {},
            onRefresh = {},
            onLoadMore = {},
            onRemoveHistoryEntry = {},
            onClearHistory = {},
            onBack = {},
            onPhotoClick = {},
            onUserClick = {},
            onCollectionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Search - Compact Dark")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreenDarkPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = true) { _, _ ->
        SearchScreenContent(
            state = UiPreviewFixtures.searchDiscoveryState,
            onQueryChange = {},
            onSearchSubmit = {},
            onSetSearchActive = {},
            onSelectColor = {},
            onSelectSearchType = {},
            onApplyFilters = {},
            onRefresh = {},
            onLoadMore = {},
            onRemoveHistoryEntry = {},
            onClearHistory = {},
            onBack = {},
            onPhotoClick = {},
            onUserClick = {},
            onCollectionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700, name = "Search - Expanded")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreenExpandedPreview() {
    PreviewScaffold(widthDp = 900, heightDp = 700, darkTheme = false) { _, _ ->
        SearchScreenContent(
            state = UiPreviewFixtures.searchDiscoveryState,
            onQueryChange = {},
            onSearchSubmit = {},
            onSetSearchActive = {},
            onSelectColor = {},
            onSelectSearchType = {},
            onApplyFilters = {},
            onRefresh = {},
            onLoadMore = {},
            onRemoveHistoryEntry = {},
            onClearHistory = {},
            onBack = {},
            onPhotoClick = {},
            onUserClick = {},
            onCollectionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Search Results - Compact Light")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchResultsScreenPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { _, _ ->
        SearchScreenContent(
            state = UiPreviewFixtures.searchResultsState,
            onQueryChange = {},
            onSearchSubmit = {},
            onSetSearchActive = {},
            onSelectColor = {},
            onSelectSearchType = {},
            onApplyFilters = {},
            onRefresh = {},
            onLoadMore = {},
            onRemoveHistoryEntry = {},
            onClearHistory = {},
            onBack = {},
            onPhotoClick = {},
            onUserClick = {},
            onCollectionClick = {},
        )
    }
}
