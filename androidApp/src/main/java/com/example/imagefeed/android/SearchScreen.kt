package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.SearchFilters
import com.example.imagefeed.presentation.SearchState
import com.example.imagefeed.presentation.SearchTab
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialQuery: String = "",
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit
) {
    val context = LocalPlatformContext.current
    val presenter = remember { MetroHelper.getUnifiedSearchPresenter() }
    DisposableEffect(presenter) {
        onDispose { presenter.clear() }
    }
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = SearchState())
    var showFiltersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            presenter.updateQuery(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0F0F11))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    TextField(
                        value = state.query,
                        onValueChange = { presenter.updateQuery(it) },
                        placeholder = { Text("Search Photos, Collections, Users...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E24),
                            unfocusedContainerColor = Color(0xFF1E1E24),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(26.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { presenter.updateQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = { showFiltersSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List, // Standard List serves as an elegant filter icon
                            contentDescription = "Filters",
                            tint = if (state.filters != SearchFilters()) Color.White else Color.Gray
                        )
                    }
                }

                // Search Category Tabs
                SecondaryTabRow(
                    selectedTabIndex = state.activeTab.ordinal,
                    containerColor = Color(0xFF0F0F11),
                    contentColor = Color.White,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(state.activeTab.ordinal),
                            color = Color.White
                        )
                    }
                ) {
                    SearchTab.values().forEach { tab ->
                        Tab(
                            selected = state.activeTab == tab,
                            onClick = { presenter.setTab(tab) },
                            text = { Text(tab.name, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0F0F11))
        ) {
            if (state.query.isBlank()) {
                // Show search suggestions and history
                SearchSuggestionsAndHistory(
                    history = state.searchHistory,
                    onItemClick = { presenter.updateQuery(it) },
                    onDeleteClick = { presenter.removeHistoryEntry(it) },
                    onClearAll = { presenter.clearHistory() }
                )
            } else if (state.isLoading) {
                when (state.activeTab) {
                    SearchTab.PHOTOS -> {
                        PhotoGridSkeleton()
                    }
                    SearchTab.COLLECTIONS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(4) {
                                CollectionMosaicCardSkeleton()
                            }
                        }
                    }
                    SearchTab.USERS -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            } else if (state.error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Search failed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.error ?: "", color = Color.Gray, fontSize = 14.sp)
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
                                onLoadMore = { presenter.loadNextPage() },
                                onPhotoClick = onPhotoClick
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
                                onLoadMore = { presenter.loadNextPage() }
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
                                onLoadMore = { presenter.loadNextPage() },
                                onUserClick = onUserClick
                            )
                        }
                    }
                }
            }

            // Filters Drawer (Modal Bottom Sheet)
            if (showFiltersSheet) {
                SearchFiltersSheet(
                    filters = state.filters,
                    onDismiss = { showFiltersSheet = false },
                    onApply = { presenter.applyFilters(it) }
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
    onClearAll: () -> Unit
) {
    val suggestions = listOf("nature", "travel", "architecture", "wallpapers", "neon", "minimalist", "urban", "textures")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENTS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "CLEAR ALL",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.bounceClick { onClearAll() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            history.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .bounceClick { onItemClick(item) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "History", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = item, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onDeleteClick(item) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("POPULAR TOPICS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { topic ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E1E24), RoundedCornerShape(18.dp))
                        .bounceClick { onItemClick(topic) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = topic, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
    onLoadMore: () -> Unit,
    onPhotoClick: (Photo) -> Unit
) {
    val listState = rememberLazyStaggeredGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 6
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
        verticalItemSpacing = 8.dp
    ) {
        items(photos, key = { it.id }) { photo ->
            val index = photos.indexOfFirst { it.id == photo.id }
            PhotoCard(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                index = index,
                photo = photo,
                onClick = { onPhotoClick(photo) },
                onUserClick = {
                    // Handled inside PhotoCard or default link click
                }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CollectionsResultList(
    collections: List<CollectionSummary>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    val context = LocalPlatformContext.current
    val listState = rememberLazyStaggeredGridState() // staggered grid with 1 column as flexible list

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(1),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalItemSpacing = 16.dp
    ) {
        items(collections, key = { it.id }) { collection ->
            val index = collections.indexOfFirst { it.id == collection.id }
            CollectionRowCard(
                collection = collection,
                modifier = Modifier.staggeredEntrance(index),
                onClick = {
                    collection.links?.html?.let { link ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$link?utm_source=ImageFeedApp&utm_medium=referral"))
                        context.startActivity(browserIntent)
                    }
                }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CollectionRowCard(
    collection: CollectionSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .bounceClick { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val coverPhoto = collection.coverPhoto
            if (coverPhoto != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = coverPhoto.urls.regular),
                    contentDescription = collection.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E24)))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = collection.title.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${collection.totalPhotos} Photos  ·  Curated by ${collection.user.name}",
                        color = Color.LightGray,
                        fontSize = 12.sp
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
    onLoadMore: () -> Unit,
    onUserClick: (User) -> Unit
) {
    val context = LocalPlatformContext.current
    val listState = rememberLazyStaggeredGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(1),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        items(users, key = { it.id }) { user ->
            val index = users.indexOfFirst { it.id == user.id }
            UserRowCard(
                user = user,
                modifier = Modifier.staggeredEntrance(index),
                onClick = {
                    onUserClick(user)
                }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun UserRowCard(
    user: User,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        modifier = modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = user.profileImage.large),
                contentDescription = user.name,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "@${user.username}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "View Profile",
                tint = Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun NoResultsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No results found.", color = Color.Gray, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    filters: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit
) {
    var selectedOrderBy by remember { mutableStateOf(filters.orderBy) }
    var selectedOrientation by remember { mutableStateOf(filters.orientation) }
    var selectedColor by remember { mutableStateOf(filters.color) }

    val colors = listOf(
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
        Pair("Blue", "blue")
    )

    val colorHexes = mapOf(
        "black" to Color.Black,
        "white" to Color.White,
        "yellow" to Color(0xFFFFEB3B),
        "orange" to Color(0xFFFF9800),
        "red" to Color(0xFFF44336),
        "purple" to Color(0xFF9C27B0),
        "magenta" to Color(0xFFE91E63),
        "green" to Color(0xFF4CAF50),
        "teal" to Color(0xFF009688),
        "blue" to Color(0xFF2196F3)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E24),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FILTERS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "RESET",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    modifier = Modifier.clickable {
                        selectedOrderBy = "relevant"
                        selectedOrientation = null
                        selectedColor = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sort Order Section
            Text("SORT BY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedOrderBy == "relevant",
                    onClick = { selectedOrderBy = "relevant" },
                    label = { Text("RELEVANT") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF2C2C35),
                        labelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedOrderBy == "latest",
                    onClick = { selectedOrderBy = "latest" },
                    label = { Text("LATEST") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF2C2C35),
                        labelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Orientation Section
            Text("ORIENTATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedOrientation == null,
                    onClick = { selectedOrientation = null },
                    label = { Text("ALL") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF2C2C35),
                        labelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedOrientation == "landscape",
                    onClick = { selectedOrientation = "landscape" },
                    label = { Text("LANDSCAPE") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF2C2C35),
                        labelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedOrientation == "portrait",
                    onClick = { selectedOrientation = "portrait" },
                    label = { Text("PORTRAIT") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF2C2C35),
                        labelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Color Section
            Text("COLOR TONE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(colors) { pair ->
                    val name = pair.first
                    val value = pair.second
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedColor = value }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (value == "black_and_white") Color.Gray
                                    else colorHexes[value] ?: Color(0xFF2C2C35)
                                )
                                .border(
                                    width = if (selectedColor == value) 2.dp else 1.dp,
                                    color = if (selectedColor == value) Color.White else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == value) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (value == "white" || value == "yellow") Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = name, color = Color.LightGray, fontSize = 10.sp)
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
                            orientation = selectedOrientation
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("APPLY FILTERS", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
