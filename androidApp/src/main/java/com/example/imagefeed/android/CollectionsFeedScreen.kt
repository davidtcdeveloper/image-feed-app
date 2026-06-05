package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.presentation.CollectionsFeedPresenter
import com.example.imagefeed.presentation.CollectionsFeedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsFeedScreen(
    presenter: CollectionsFeedPresenter,
    onCollectionClick: (PhotoCollection) -> Unit,
    onSearchClick: () -> Unit
) {
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = CollectionsFeedState())
    val listState = rememberLazyListState()

    // Infinite scrolling logic
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            presenter.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "CURATED COLLECTIONS",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Image(
                            painter = rememberAsyncImagePainter(model = android.R.drawable.ic_menu_search),
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F0F11)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.collections.isEmpty() && state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (state.collections.isEmpty() && state.error != null) {
                ErrorView(error = state.error!!, onRetry = { presenter.refresh() })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.collections, key = { it.id }) { collection ->
                        CollectionMosaicCard(
                            collection = collection,
                            onClick = { onCollectionClick(collection) }
                        )
                    }

                    if (state.isLoading) {
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
}

@Composable
fun CollectionMosaicCard(
    collection: PhotoCollection,
    onClick: () -> Unit
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Calculate dimensions for preview hotlinking compliance
    val screenWidthDp = configuration.screenWidthDp
    val cardWidthPx = with(density) { (screenWidthDp - 24).dp.roundToPx() }
    val sideWidthPx = cardWidthPx / 3

    // Use BlurHash from cover photo as card background placeholder
    val blurHash = collection.coverPhoto?.blurHash
    val placeholderBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, blurHash) {
        if (blurHash != null) {
            value = BlurHashDecoder.decode(blurHash, 32, 18)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mosaic Grid: Left large cover, right two smaller thumbnails
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Left Image: Cover Photo (weight 2f)
                val coverUrl = collection.coverPhoto?.urls?.raw?.let { "$it&w=${cardWidthPx * 2 / 3}&q=80&auto=format" }
                    ?: collection.previewPhotos?.firstOrNull()?.urls?.small

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .background(Color(0xFF2C2C35))
                ) {
                    if (coverUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(coverUrl)
                                    .crossfade(true)
                                    .build(),
                                placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) }
                            ),
                            contentDescription = collection.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Column: Two thumbnails (weight 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Top Thumbnail
                    val topThumbUrl = collection.previewPhotos?.getOrNull(1)?.urls?.small
                        ?: collection.previewPhotos?.getOrNull(0)?.urls?.small
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C35))
                    ) {
                        if (topThumbUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data("$topThumbUrl&w=$sideWidthPx&q=80&auto=format")
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Preview photo 1",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Thumbnail
                    val bottomThumbUrl = collection.previewPhotos?.getOrNull(2)?.urls?.small
                        ?: collection.previewPhotos?.getOrNull(1)?.urls?.small
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C35))
                    ) {
                        if (bottomThumbUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data("$bottomThumbUrl&w=$sideWidthPx&q=80&auto=format")
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Preview photo 2",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Collection Information Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = collection.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val description = collection.description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Curator Profile & Attribution
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val utmProfile = "${collection.user.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(utmProfile))
                                context.startActivity(browserIntent)
                            }
                    ) {
                        val avatarPainter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(collection.user.profileImage.small)
                                .crossfade(true)
                                .build()
                        )

                        Image(
                            painter = avatarPainter,
                            contentDescription = "Curator profile image",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Curated by",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = collection.user.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Total photos badge
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${collection.totalPhotos} Photos",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
