package com.example.imagefeed.android.search

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.android.calculateBottomContentPadding
import com.example.imagefeed.android.calculateCollectionGridColumns
import com.example.imagefeed.android.calculateCollectionMosaicCardWidthPx
import com.example.imagefeed.android.calculateGridColumns
import com.example.imagefeed.android.calculatePhotoItemWidthPx
import com.example.imagefeed.android.calculateUserGridColumns
import com.example.imagefeed.android.components.PhotoCard
import com.example.imagefeed.android.designsystem.LocalAppSharedTransition
import com.example.imagefeed.android.designsystem.LocalSatinHeaderPadding
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.theme.ExpressiveWavyProgressIndicator
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val PAGINATION_PHOTO_THRESHOLD = 6
private const val PAGINATION_COLLECTION_THRESHOLD = 4
private const val PAGINATION_USER_THRESHOLD = 4

private val CollectionRowScrimBrush =
    Brush.verticalGradient(
        listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.65f),
        ),
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosResultGrid(
    photos: List<Photo>,
    isLoadingMore: Boolean,
    hasReachedEnd: Boolean = false,
    reselectEvents: SharedFlow<NavKey>? = null,
    onLoadMore: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
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
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_PHOTO_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                currentOnLoadMore()
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
                top = LocalSatinHeaderPadding.current + 8.dp,
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
                index = index,
                photo = photo,
                itemWidthPx = itemWidthPx,
                isTransitionSource = photo.id == selectedPhotoId,
                onClick = {
                    selectedPhotoId = photo.id
                    onPhotoClick(photo)
                },
                onUserClick = {
                    onUserClick(photo.user)
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
                    ExpressiveWavyProgressIndicator()
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
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_COLLECTION_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                currentOnLoadMore()
            }
    }

    LazyVerticalStaggeredGrid(
        columns = calculateCollectionGridColumns(),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 12.dp,
                top = LocalSatinHeaderPadding.current + 12.dp,
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
                    ExpressiveWavyProgressIndicator()
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
    // UI-element transition state gating the avatar sharedElement; ViewModel remains the SSOT.
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key is AppRoute.Search) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val currentLoading by rememberUpdatedState(isLoadingMore)
    val currentHasReachedEnd by rememberUpdatedState(hasReachedEnd)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - PAGINATION_USER_THRESHOLD
        }.distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad && !currentLoading && !currentHasReachedEnd }
            .collect {
                currentOnLoadMore()
            }
    }

    LazyVerticalStaggeredGrid(
        columns = calculateUserGridColumns(),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 12.dp,
                top = LocalSatinHeaderPadding.current + 12.dp,
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
                isTransitionSource = user.username == selectedUsername,
                onClick = {
                    selectedUsername = user.username
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
                    ExpressiveWavyProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserRowCard(
    user: User,
    modifier: Modifier = Modifier,
    isTransitionSource: Boolean = false,
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
            val sharedTransition = LocalAppSharedTransition.current
            val baseAvatarModifier = Modifier.size(54.dp)
            val avatarModifier =
                if (isTransitionSource && sharedTransition != null) {
                    with(sharedTransition.sharedTransitionScope) {
                        baseAvatarModifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "avatar_${user.username}"),
                            animatedVisibilityScope = sharedTransition.animatedVisibilityScope,
                        )
                    }
                } else {
                    baseAvatarModifier
                }
            Image(
                painter = rememberAsyncImagePainter(model = avatarRequest),
                contentDescription = user.name,
                modifier =
                    avatarModifier
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
