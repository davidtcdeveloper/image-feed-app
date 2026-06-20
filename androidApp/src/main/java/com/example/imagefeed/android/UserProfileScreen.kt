package com.example.imagefeed.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserStats
import com.example.imagefeed.presentation.ProfileTab
import com.example.imagefeed.presentation.UserProfileState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.android.util.UserProfileHeaderSkeleton
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun UserProfileScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    username: String,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onCollectionClick: (PhotoCollection) -> Unit
) {
    val context = LocalPlatformContext.current
    val presenter = remember(username) { MetroHelper.getUserProfilePresenter(username) }
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = UserProfileState())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.user?.name?.uppercase() ?: "PHOTOGRAPHER",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    state.user?.let { user ->
                        IconButton(onClick = {
                            val profileUrl = "${user.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl))
                            context.startActivity(browserIntent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Open in Browser",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F11)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0F0F11))
        ) {
            if (state.isHeaderLoading && state.user == null) {
                UserProfileHeaderSkeleton()
            } else if (state.error != null && state.user == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Failed to load profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.error ?: "", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { presenter.loadProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                state.user?.let { user ->
                    UserProfileContent(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        user = user,
                        state = state,
                        onTabSelect = { presenter.selectTab(it) },
                        onLoadMore = { presenter.loadNextPage() },
                        onPhotoClick = onPhotoClick,
                        onCollectionClick = onCollectionClick
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
    onCollectionClick: (PhotoCollection) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Sticky Profile Info Card
        ProfileHeaderSection(user = user)

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Navigation Tab Row
        ProfileTabSelector(
            activeTab = state.activeTab,
            user = user,
            onTabSelect = onTabSelect
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-Tab Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp, max = 2000.dp)
        ) {
            when (state.activeTab) {
                ProfileTab.PORTFOLIO -> {
                    PortfolioTabContent(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photos = state.portfolioPhotos,
                        isLoading = state.isLoadingContent,
                        onLoadMore = onLoadMore,
                        onPhotoClick = onPhotoClick
                    )
                }
                ProfileTab.LIKES -> {
                    PortfolioTabContent(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photos = state.likedPhotos,
                        isLoading = state.isLoadingContent,
                        onLoadMore = onLoadMore,
                        onPhotoClick = onPhotoClick
                    )
                }
                ProfileTab.COLLECTIONS -> {
                    CollectionsTabContent(
                        collections = state.collections,
                        isLoading = state.isLoadingContent,
                        onLoadMore = onLoadMore,
                        onCollectionClick = onCollectionClick
                    )
                }
                ProfileTab.INSIGHTS -> {
                    InsightsTabContent(
                        stats = state.stats,
                        isLoading = state.isLoadingStats,
                        error = state.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderSection(user: User) {
    val context = LocalPlatformContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImage.large),
            contentDescription = user.name,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "@${user.username}",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        if (!user.location.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = user.location ?: "",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }

        if (!user.bio.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.bio ?: "",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Social Media Badges
        val hasInstagram = !user.social?.instagramUsername.isNullOrEmpty()
        val hasTwitter = !user.social?.twitterUsername.isNullOrEmpty()
        val hasPortfolio = !user.social?.portfolioUrl.isNullOrEmpty()

        if (hasInstagram || hasTwitter || hasPortfolio) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasInstagram) {
                    SocialBadge(label = "Instagram", handle = user.social?.instagramUsername ?: "") {
                        val igUrl = "instagram://user?username=${user.social?.instagramUsername}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(igUrl))
                        intent.setPackage("com.instagram.android")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to web browser
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/${user.social?.instagramUsername}")))
                        }
                    }
                }
                if (hasTwitter) {
                    SocialBadge(label = "Twitter", handle = user.social?.twitterUsername ?: "") {
                        val twUrl = "twitter://user?screen_name=${user.social?.twitterUsername}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(twUrl))
                        intent.setPackage("com.twitter.android")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to web browser
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${user.social?.twitterUsername}")))
                        }
                    }
                }
                if (hasPortfolio) {
                    SocialBadge(label = "Website", handle = "Link") {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(user.social?.portfolioUrl))
                        context.startActivity(webIntent)
                    }
                }
            }
        }
    }
}

@Composable
fun SocialBadge(label: String, handle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1E1E24), RoundedCornerShape(16.dp))
            .bounceClick { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label: @$handle".uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ProfileTabSelector(
    activeTab: ProfileTab,
    user: User,
    onTabSelect: (ProfileTab) -> Unit
) {
    SecondaryTabRow(
        selectedTabIndex = activeTab.ordinal,
        containerColor = Color(0xFF0F0F11),
        contentColor = Color.White,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(activeTab.ordinal),
                color = Color.White
            )
        }
    ) {
        ProfileTab.values().forEach { tab ->
            val isSelected = activeTab == tab
            val tabLabel = when (tab) {
                ProfileTab.PORTFOLIO -> "PHOTOS (${user.totalPhotos ?: 0})"
                ProfileTab.LIKES -> "LIKES (${user.totalLikes ?: 0})"
                ProfileTab.COLLECTIONS -> "COLLECTIONS (${user.totalCollections ?: 0})"
                ProfileTab.INSIGHTS -> "INSIGHTS"
            }
            Tab(
                selected = isSelected,
                onClick = { onTabSelect(tab) },
                text = {
                    Text(
                        text = tabLabel,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PortfolioTabContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photos: List<Photo>,
    isLoading: Boolean,
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (photos.isEmpty() && isLoading) {
            PhotoGridSkeleton()
        } else if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No photos found.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = calculateGridColumns(),
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 2000.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                userScrollEnabled = false // Scroll is controlled by parent vertical scroll
            ) {
                items(photos, key = { it.id }) { photo ->
                    val index = photos.indexOfFirst { it.id == photo.id }
                    PhotoCard(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        index = index,
                        photo = photo,
                        onClick = { onPhotoClick(photo) },
                        onUserClick = {} // Disable click on same user to avoid looping
                    )
                }

                if (isLoading) {
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
    }
}

@Composable
fun CollectionsTabContent(
    collections: List<PhotoCollection>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onCollectionClick: (PhotoCollection) -> Unit
) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (collections.isEmpty() && isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    CollectionMosaicCardSkeleton()
                }
            }
        } else if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No collections found.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(1),
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 2000.dp),
                contentPadding = PaddingValues(12.dp),
                verticalItemSpacing = 16.dp,
                userScrollEnabled = false
            ) {
                items(collections, key = { it.id }) { col ->
                    val index = collections.indexOfFirst { it.id == col.id }
                    CollectionRowLayout(
                        collection = col,
                        modifier = Modifier.staggeredEntrance(index),
                        onClick = { onCollectionClick(col) }
                    )
                }

                if (isLoading) {
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
    }
}

@Composable
fun CollectionRowLayout(
    collection: PhotoCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .bounceClick(onClick = onClick)
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
fun InsightsTabContent(
    stats: UserStats?,
    isLoading: Boolean,
    error: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (error != null && stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load insights.", color = Color.Gray, fontSize = 14.sp)
            }
        } else if (stats != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Headline Consolidated Stats Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Total Views",
                        value = formatStatValue(stats.views.total),
                        icon = Icons.Default.Info
                    )
                    StatItem(
                        label = "Total Downloads",
                        value = formatStatValue(stats.downloads.total),
                        icon = Icons.Default.LocationOn // placeholder icon
                    )
                }

                // Interactive Views Chart
                stats.views.historical?.values?.let { viewsList ->
                    if (viewsList.isNotEmpty()) {
                        Text(
                            text = "VIEWS TRENDS (LAST 30 DAYS)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InteractiveTimelineChart(
                                    data = viewsList.map { it.value.toFloat() },
                                    dates = viewsList.map { it.date },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Downloads Chart
                stats.downloads.historical?.values?.let { downloadsList ->
                    if (downloadsList.isNotEmpty()) {
                        Text(
                            text = "DOWNLOADS TRENDS (LAST 30 DAYS)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InteractiveTimelineChart(
                                    data = downloadsList.map { it.value.toFloat() },
                                    dates = downloadsList.map { it.date },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveTimelineChart(
    data: List<Float>,
    dates: List<String>,
    modifier: Modifier = Modifier
) {
    var dragX by remember { mutableStateOf<Float?>(null) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val maxVal = data.maxOrNull() ?: 1f
    val minVal = data.minOrNull() ?: 0f
    val diff = if (maxVal == minVal) 1f else maxVal - minVal

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "drawChart"
    )

    Column(modifier = modifier) {
        // Overlay displaying hovered detail
        if (dragX != null && data.isNotEmpty()) {
            val width = 300.dp // Approx width placeholder
            val density = LocalDensity.current
            val stepX = with(density) { (300.dp / (data.size - 1).coerceAtLeast(1)).toPx() }
            val index = (dragX!! / stepX).roundToInt().coerceIn(0, data.size - 1)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dates[index],
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${data[index].toInt()} units",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Drag curve to inspect daily stats", color = Color.Gray, fontSize = 11.sp)
                Text("Peak: ${maxVal.toInt()}", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragX = offset.x
                            // Vibrate gracefully using system haptics
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                vibratorManager?.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            }

                            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX = change.position.x
                        },
                        onDragEnd = {
                            dragX = null
                        },
                        onDragCancel = {
                            dragX = null
                        }
                    )
                }
        ) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height

            val stepX = width / (data.size - 1).coerceAtLeast(1)

            val points = data.mapIndexed { idx, value ->
                val x = idx * stepX
                val y = height - ((value - minVal) / diff) * height * 0.85f - (height * 0.05f)
                Offset(x, y * animProgress)
            }

            // Fill gradient
            val fillPath = Path().apply {
                moveTo(0f, height)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Main stroke
            val strokePath = Path().apply {
                points.forEachIndexed { idx, point ->
                    if (idx == 0) moveTo(point.x, point.y)
                    else lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = strokePath,
                color = Color.White,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw interaction slider line if dragging
            dragX?.let { xOffset ->
                val index = (xOffset / stepX).roundToInt().coerceIn(0, data.size - 1)
                val hoverPoint = points[index]
                
                // Draw vertical indicator line
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(hoverPoint.x, 0f),
                    end = Offset(hoverPoint.x, height),
                    strokeWidth = 1.dp.toPx()
                )

                // Highlighted interaction dot
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = hoverPoint
                )
                drawCircle(
                    color = Color(0xFF1E1E24),
                    radius = 3.dp.toPx(),
                    center = hoverPoint
                )
            }
        }
    }
}
