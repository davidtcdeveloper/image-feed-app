package com.example.imagefeed.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import com.example.imagefeed.android.util.CollectionMosaicCardSkeleton
import com.example.imagefeed.android.util.PhotoGridSkeleton
import com.example.imagefeed.android.util.UserProfileHeaderSkeleton
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserStats
import com.example.imagefeed.presentation.profile.ProfileTab
import com.example.imagefeed.presentation.profile.UserProfileEffect
import com.example.imagefeed.presentation.profile.UserProfileIntent
import com.example.imagefeed.presentation.profile.UserProfileState
import com.example.imagefeed.presentation.profile.UserProfileViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
    ) {
        // Sticky Profile Info Card
        ProfileHeaderSection(user = user)

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Navigation Tab Row
        ProfileTabSelector(
            activeTab = state.activeTab,
            user = user,
            onTabSelect = onTabSelect,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-Tab Content Area
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp, max = 2000.dp),
        ) {
            when (state.activeTab) {
                ProfileTab.PORTFOLIO -> {
                    PortfolioTabContent(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photos = state.portfolioPhotos,
                        isLoading = state.isLoadingContent,
                        hasReachedEnd = state.portfolioReachedEnd,
                        onLoadMore = onLoadMore,
                        onPhotoClick = onPhotoClick,
                        onUserClick = onUserClick,
                        showAttribution = false,
                    )
                }
                ProfileTab.LIKES -> {
                    PortfolioTabContent(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photos = state.likedPhotos,
                        isLoading = state.isLoadingContent,
                        hasReachedEnd = state.likesReachedEnd,
                        onLoadMore = onLoadMore,
                        onPhotoClick = onPhotoClick,
                        onUserClick = onUserClick,
                        showAttribution = true,
                    )
                }
                ProfileTab.COLLECTIONS -> {
                    CollectionsTabContent(
                        collections = state.collections,
                        isLoading = state.isLoadingContent,
                        hasReachedEnd = state.collectionsReachedEnd,
                        onLoadMore = onLoadMore,
                        onCollectionClick = onCollectionClick,
                    )
                }
                ProfileTab.INSIGHTS -> {
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

@Composable
fun ProfileHeaderSection(user: User) {
    val context = LocalPlatformContext.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImage.large),
            contentDescription = user.name,
            modifier =
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )

        Text(
            text = "@${user.username}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        if (!user.location.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.content_desc_location),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = user.location ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (!user.bio.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.bio ?: "",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Social Media Assist Chips
        val instagramHandle =
            user.social
                ?.instagramUsername
                ?.trim()
                ?.removePrefix("@")
                ?.ifBlank { null }
        val twitterHandle =
            user.social
                ?.twitterUsername
                ?.trim()
                ?.removePrefix("@")
                ?.ifBlank { null }
        val portfolioUrl =
            user.social
                ?.portfolioUrl
                ?.trim()
                ?.ifBlank { null }
                ?: user.portfolioUrl?.trim()?.ifBlank { null }

        val hasInstagram = instagramHandle != null
        val hasTwitter = twitterHandle != null
        val hasPortfolio = portfolioUrl != null

        if (hasInstagram || hasTwitter || hasPortfolio) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (instagramHandle != null) {
                    AssistChip(
                        onClick = {
                            val igUrl = "instagram://user?username=$instagramHandle"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(igUrl))
                            intent.setPackage("com.instagram.android")
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    // Fallback to web browser
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$instagramHandle")),
                                    )
                                } catch (_: Exception) {
                                    // Ignore if no handler available
                                }
                            }
                        },
                        label = { Text("@$instagramHandle") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.profile_social_instagram_accessibility),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
                if (twitterHandle != null) {
                    AssistChip(
                        onClick = {
                            val twUrl = "twitter://user?screen_name=$twitterHandle"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(twUrl))
                            intent.setPackage("com.twitter.android")
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    // Fallback to web browser
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/$twitterHandle")),
                                    )
                                } catch (_: Exception) {
                                    // Ignore if no handler available
                                }
                            }
                        },
                        label = { Text("@$twitterHandle") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = stringResource(R.string.profile_social_twitter_accessibility),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
                if (portfolioUrl != null) {
                    val formattedUrl =
                        when {
                            portfolioUrl.startsWith(
                                "http://",
                                ignoreCase = true,
                            ) ||
                                portfolioUrl.startsWith("https://", ignoreCase = true) -> portfolioUrl
                            else -> "https://$portfolioUrl"
                        }
                    AssistChip(
                        onClick = {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                                context.startActivity(webIntent)
                            } catch (_: Exception) {
                                // Ignore if no handler available
                            }
                        },
                        label = { Text(stringResource(R.string.profile_social_website_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.profile_social_website_accessibility),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTabSelector(
    activeTab: ProfileTab,
    user: User,
    onTabSelect: (ProfileTab) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeTab) {
        val index = ProfileTab.entries.indexOf(activeTab)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(
            items = ProfileTab.entries,
            key = { it.name },
        ) { tab ->
            val isSelected = activeTab == tab
            val (label, count) =
                when (tab) {
                    ProfileTab.PORTFOLIO -> stringResource(R.string.profile_tab_photos) to (user.totalPhotos ?: 0)
                    ProfileTab.LIKES -> stringResource(R.string.profile_tab_likes) to (user.totalLikes ?: 0)
                    ProfileTab.COLLECTIONS -> stringResource(R.string.profile_tab_collections) to (user.totalCollections ?: 0)
                    ProfileTab.INSIGHTS -> stringResource(R.string.profile_tab_insights) to null
                }

            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 16.dp else 28.dp,
                label = "tabCornerRadius",
            )
            val containerColor by animateColorAsState(
                targetValue =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                label = "tabContainerColor",
            )
            val contentColor by animateColorAsState(
                targetValue =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                label = "tabContentColor",
            )

            Surface(
                onClick = { onTabSelect(tab) },
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                contentColor = contentColor,
                border =
                    if (!isSelected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    } else {
                        null
                    },
                modifier =
                    Modifier.semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (count != null) {
                        val badgeContainerColor by animateColorAsState(
                            targetValue =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            label = "tabBadgeContainerColor",
                        )
                        val badgeContentColor by animateColorAsState(
                            targetValue =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            label = "tabBadgeContentColor",
                        )
                        Badge(
                            containerColor = badgeContainerColor,
                            contentColor = badgeContentColor,
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
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
    hasReachedEnd: Boolean = false,
    onLoadMore: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit = {},
    showAttribution: Boolean = true,
) {
    val listState = rememberLazyStaggeredGridState()
    val currentLoading by rememberUpdatedState(isLoading)
    val currentHasReachedEnd by rememberUpdatedState(hasReachedEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                onLoadMore()
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (photos.isEmpty() && isLoading) {
            PhotoGridSkeleton()
        } else if (photos.isEmpty()) {
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
        } else {
            val itemWidthPx = calculatePhotoItemWidthPx()
            // UI-element transition state: gates Modifier.sharedElement so idle lazy items avoid tracking overhead during flings.
            // Does not represent business selection; ViewModel remains the SSOT for profile data.
            var selectedPhotoId by rememberSaveable { mutableStateOf<String?>(null) }

            LazyVerticalStaggeredGrid(
                columns = calculateGridColumns(),
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 2000.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                userScrollEnabled = false, // Scroll is controlled by parent vertical scroll
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
                        onUserClick = { onUserClick(photo.user) },
                        showAttribution = showAttribution,
                    )
                }

                if (isLoading) {
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

@Composable
fun CollectionsTabContent(
    collections: List<PhotoCollection>,
    isLoading: Boolean,
    hasReachedEnd: Boolean = false,
    onLoadMore: () -> Unit,
    onCollectionClick: (PhotoCollection) -> Unit,
) {
    val listState = rememberLazyStaggeredGridState()
    val currentLoading by rememberUpdatedState(isLoading)
    val currentHasReachedEnd by rememberUpdatedState(hasReachedEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                onLoadMore()
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (collections.isEmpty() && isLoading) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(3) {
                    CollectionMosaicCardSkeleton()
                }
            }
        } else if (collections.isEmpty()) {
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
        } else {
            LazyVerticalStaggeredGrid(
                columns = calculateCollectionGridColumns(),
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 2000.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 16.dp,
                userScrollEnabled = false,
            ) {
                itemsIndexed(
                    collections,
                    key = { _, col -> col.id },
                    contentType = { _, _ -> "collection_card" },
                ) { index, col ->
                    CollectionRowLayout(
                        collection = col,
                        modifier = Modifier.staggeredEntrance(index),
                        onClick = { onCollectionClick(col) },
                    )
                }

                if (isLoading) {
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

@Composable
fun CollectionRowLayout(
    collection: PhotoCollection,
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
                .bounceClick(onClick = onClick),
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
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
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
fun InsightsTabContent(
    stats: UserStats?,
    isLoading: Boolean,
    error: com.example.imagefeed.model.AppError?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && stats == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null && stats == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.profile_insights_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (stats != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                // Headline Consolidated Stats Panel
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.profile_stat_total_views),
                        value = formatStatValue(stats.views.total),
                        icon = Icons.Default.Visibility,
                        valueStyle = MaterialTheme.typography.titleMedium,
                        labelStyle = MaterialTheme.typography.bodySmall,
                    )
                    StatItem(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.profile_stat_total_downloads),
                        value = formatStatValue(stats.downloads.total),
                        icon = Icons.Default.FileDownload,
                        valueStyle = MaterialTheme.typography.titleMedium,
                        labelStyle = MaterialTheme.typography.bodySmall,
                    )
                }

                // Interactive Views Chart
                stats.views.historical?.values?.let { viewsList ->
                    if (viewsList.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.profile_chart_views_trend),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InteractiveTimelineChart(
                                    data = viewsList.map { it.value.toFloat() },
                                    dates = viewsList.map { it.date },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
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
                            text = stringResource(R.string.profile_chart_downloads_trend),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InteractiveTimelineChart(
                                    data = downloadsList.map { it.value.toFloat() },
                                    dates = downloadsList.map { it.date },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
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
    modifier: Modifier = Modifier,
) {
    var dragX by remember { mutableStateOf<Float?>(null) }
    val context = LocalContext.current
    val chartColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = data.maxOrNull() ?: 1f
    val minVal = data.minOrNull() ?: 0f
    val diff = if (maxVal == minVal) 1f else maxVal - minVal

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "drawChart",
    )

    Column(modifier = modifier) {
        // Overlay displaying hovered detail
        if (dragX != null && data.isNotEmpty()) {
            val density = LocalDensity.current
            val stepX = with(density) { (300.dp / (data.size - 1).coerceAtLeast(1)).toPx() }
            val index = (dragX!! / stepX).roundToInt().coerceIn(0, data.size - 1)

            val formattedDate =
                remember(dates, index) {
                    try {
                        java.time.LocalDate
                            .parse(dates[index])
                            .format(
                                java.time.format.DateTimeFormatter
                                    .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM),
                            )
                    } catch (_: Exception) {
                        dates[index]
                    }
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formattedDate,
                    color = onSurfaceVariantColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.profile_chart_units, data[index].toInt()),
                    color = onSurfaceColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                )
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_chart_drag_prompt),
                    color = onSurfaceVariantColor,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = stringResource(R.string.details_metric_peak, formatStatValue(maxVal.toInt())),
                    color = onSurfaceColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        Canvas(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragX = offset.x
                                // Vibrate gracefully using system haptics
                                val vibrator =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                        vibratorManager?.defaultVibrator
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                    }

                                vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragX = change.position.x
                            },
                            onDragEnd = {
                                dragX = null
                            },
                            onDragCancel = {
                                dragX = null
                            },
                        )
                    },
        ) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height

            val stepX = width / (data.size - 1).coerceAtLeast(1)

            val points =
                data.mapIndexed { idx, value ->
                    val x = idx * stepX
                    val y = height - ((value - minVal) / diff) * height * 0.85f - (height * 0.05f)
                    Offset(x, y * animProgress)
                }

            // Fill gradient
            val fillPath =
                Path().apply {
                    moveTo(0f, height)
                    points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(width, height)
                    close()
                }
            drawPath(
                path = fillPath,
                brush =
                    Brush.verticalGradient(
                        colors = listOf(chartColor.copy(alpha = 0.22f), Color.Transparent),
                        startY = 0f,
                        endY = height,
                    ),
            )

            // Main stroke
            val strokePath =
                Path().apply {
                    points.forEachIndexed { idx, point ->
                        if (idx == 0) {
                            moveTo(point.x, point.y)
                        } else {
                            lineTo(point.x, point.y)
                        }
                    }
                }
            drawPath(
                path = strokePath,
                color = chartColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            // Draw interaction slider line if dragging
            dragX?.let { xOffset ->
                val index = (xOffset / stepX).roundToInt().coerceIn(0, data.size - 1)
                val hoverPoint = points[index]

                // Draw vertical indicator line
                drawLine(
                    color = chartColor.copy(alpha = 0.4f),
                    start = Offset(hoverPoint.x, 0f),
                    end = Offset(hoverPoint.x, height),
                    strokeWidth = 1.dp.toPx(),
                )

                // Highlighted interaction dot
                drawCircle(
                    color = chartColor,
                    radius = 7.dp.toPx(),
                    center = hoverPoint,
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 3.dp.toPx(),
                    center = hoverPoint,
                )
            }
        }
    }
}
