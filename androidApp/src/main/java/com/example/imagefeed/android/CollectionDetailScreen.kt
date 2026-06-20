package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.presentation.CollectionDetailState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.staggeredEntrance

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    collectionId: String,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onCollectionClick: (String) -> Unit
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
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = gridState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            presenter.loadNextPhotosPage()
        }
    }

    // Sticky Header transparency control based on scroll position
    val showCollapsedTitle = remember {
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
                        exit = fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.collection?.title ?: "Collection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                            state.collection?.let {
                                Text(
                                    text = "${it.totalPhotos} Photos",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = rememberAsyncImagePainter(model = android.R.drawable.ic_media_previous),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showCollapsedTitle.value) Color(0xEE0F0F11) else Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F11))
                .padding(bottom = paddingValues.calculateBottomPadding()) // top is drawn fully behind transparent top bar
        ) {
            if (state.photos.isEmpty() && state.isLoadingPhotos && state.isHeaderLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = calculateGridColumns(),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    // Header Item: Spans full width
                    item(span = StaggeredGridItemSpan.FullLine) {
                        CollectionDetailHeader(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            collection = state.collection,
                            isHeaderLoading = state.isHeaderLoading
                        )
                    }

                    // Related Collections Carousel: Spans full width
                    if (state.related.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            RelatedCollectionsCarousel(
                                related = state.related,
                                onCollectionClick = onCollectionClick
                            )
                        }
                    }

                    // Title separator for photos list
                    if (state.photos.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "Photos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp)
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
                                }
                            )
                        }
                    }

                    if (state.isLoadingPhotos) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailHeader(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    collection: PhotoCollection?,
    isHeaderLoading: Boolean
) {
    val context = LocalPlatformContext.current
    if (collection == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF1E1E24))
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
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Blurred Background Cover Photo to act as beautiful backdrop
        collection.coverPhoto?.urls?.regular?.let { coverUrl ->
            with(sharedTransitionScope) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "col_cover_${collection.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .blur(20.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Overlay Gradient for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color(0xFF0F0F11).copy(alpha = 0.85f),
                            Color(0xFF0F0F11)
                        )
                    )
                )
        )

        // Contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Curator details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        val utmProfile = "${collection.user.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(utmProfile))
                        context.startActivity(browserIntent)
                    }
                    .padding(bottom = 12.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = collection.user.profileImage.medium),
                    contentDescription = collection.user.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Curated by",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = collection.user.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Collection Title & Count
            Text(
                text = collection.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 28.sp
            )

            Text(
                text = "${collection.totalPhotos} Photos",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            // Description
            val description = collection.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun RelatedCollectionsCarousel(
    related: List<PhotoCollection>,
    onCollectionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Related Collections",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(related, key = { it.id }) { item ->
                RelatedCollectionCard(
                    collection = item,
                    onClick = { onCollectionClick(item.id) }
                )
            }
        }
    }
}

@Composable
fun RelatedCollectionCard(
    collection: PhotoCollection,
    onClick: () -> Unit
) {
    val context = LocalPlatformContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(180.dp)
            .height(130.dp)
            .bounceClick(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val coverUrl = collection.coverPhoto?.urls?.small
            if (coverUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = collection.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Dark semi-transparent overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Collection text details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = collection.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${collection.totalPhotos} Photos",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}
