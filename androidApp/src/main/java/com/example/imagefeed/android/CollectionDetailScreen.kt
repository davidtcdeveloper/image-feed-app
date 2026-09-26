package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.presentation.CollectionDetailState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    collectionId: String,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onCollectionClick: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val metroHelper = remember { com.example.imagefeed.di.MetroHelper }
    val presenter = remember(collectionId) { metroHelper.getCollectionDetailPresenter(collectionId) }
    DisposableEffect(presenter) {
        onDispose { presenter.clear() }
    }
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = CollectionDetailState())

    val gridState = rememberLazyStaggeredGridState()

    // Determine when to trigger infinite pre-fetching
    val shouldLoadMore =
        remember {
            derivedStateOf {
                val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItems = gridState.layoutInfo.totalItemsCount
                if (lastVisibleItem == null || totalItems == 0) {
                    false
                } else {
                    lastVisibleItem.index >= totalItems - 5
                }
            }
        }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            presenter.loadNextPhotosPage()
        }
    }

    // Sticky Header transparency control based on scroll position
    val showCollapsedTitle =
        remember {
            derivedStateOf {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 300
            }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showCollapsedTitle.value,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.collection?.title ?: "Collection",
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp),
                            )
                            state.collection?.let {
                                Text(
                                    text = "${it.totalPhotos} Photos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            if (showCollapsedTitle.value) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            } else {
                                Color.Transparent
                            },
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor =
                            if (showCollapsedTitle.value) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White
                            },
                    ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = paddingValues.calculateBottomPadding()), // top is drawn fully behind transparent top bar
        ) {
            if (state.photos.isEmpty() && state.isLoadingPhotos && state.isHeaderLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = calculateGridColumns(),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    // Header Item: Spans full width
                    item(span = StaggeredGridItemSpan.FullLine) {
                        CollectionDetailHeader(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            collection = state.collection,
                        )
                    }

                    // Related Collections Carousel: Spans full width
                    if (state.related.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            RelatedCollectionsCarousel(
                                related = state.related,
                                onCollectionClick = onCollectionClick,
                            )
                        }
                    }

                    // Title separator for photos list
                    if (state.photos.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "Photos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                            )
                        }
                    }

                    // Photos grid
                    items(state.photos, key = { it.id }) { photo ->
                        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                            val index = state.photos.indexOfFirst { it.id == photo.id }
                            PhotoCard(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                index = index,
                                photo = photo,
                                onClick = { onPhotoClick(photo) },
                                onUserClick = {
                                    val userProfileUrl = "${photo.user.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(userProfileUrl))
                                    context.startActivity(browserIntent)
                                },
                            )
                        }
                    }

                    if (state.isLoadingPhotos) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
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
fun CollectionDetailHeader(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    collection: PhotoCollection?,
) {
    val context = LocalPlatformContext.current
    if (collection == null) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
        )
        return
    }

    val blurHash = collection.coverPhoto?.blurHash
    val placeholderBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, blurHash) {
        if (blurHash != null) {
            value = BlurHashDecoder.decode(blurHash, 16, 12)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(320.dp),
    ) {
        // Blurred Background Cover Photo to act as beautiful backdrop
        collection.coverPhoto?.urls?.regular?.let { coverUrl ->
            with(sharedTransitionScope) {
                Image(
                    painter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(coverUrl)
                                    .crossfade(true)
                                    .build(),
                            placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
                        ),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "col_cover_${collection.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ).blur(20.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Overlay Gradient for contrast
        val surfaceColor = MaterialTheme.colorScheme.surface
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    surfaceColor.copy(alpha = 0.85f),
                                    surfaceColor,
                                ),
                        ),
                    ),
        )

        // Contents
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Curator details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clickable {
                            val utmProfile = "${collection.user.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(utmProfile))
                            context.startActivity(browserIntent)
                        }.padding(bottom = 12.dp),
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = collection.user.profileImage.medium),
                    contentDescription = collection.user.name,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Curated by",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = collection.user.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Collection Title & Count
            val isExpandedScreen = LocalConfiguration.current.screenWidthDp >= 600
            val titleStyle =
                if (isExpandedScreen) {
                    MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                }
            Text(
                text = collection.title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "${collection.totalPhotos} Photos",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            // Description
            val description = collection.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun RelatedCollectionsCarousel(
    related: List<PhotoCollection>,
    onCollectionClick: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
    ) {
        Text(
            text = "Related Collections",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(related, key = { it.id }) { item ->
                RelatedCollectionCard(
                    collection = item,
                    onClick = { onCollectionClick(item.id) },
                )
            }
        }
    }
}

@Composable
fun RelatedCollectionCard(
    collection: PhotoCollection,
    onClick: () -> Unit,
) {
    val context = LocalPlatformContext.current
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            Modifier
                .width(180.dp)
                .height(130.dp)
                .bounceClick(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val coverUrl = collection.coverPhoto?.urls?.small
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
                        ),
                    contentDescription = collection.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Dark semi-transparent overlay
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            ),
                        ),
            )

            // Collection text details
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${collection.totalPhotos} Photos",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
