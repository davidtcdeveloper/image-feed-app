package com.example.imagefeed.android.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.android.calculateCollectionMosaicCardWidthPx
import com.example.imagefeed.android.designsystem.LocalAppSharedTransition
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionMosaicCard(
    sharedTransitionScope: SharedTransitionScope? = LocalAppSharedTransition.current?.sharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = LocalAppSharedTransition.current?.animatedVisibilityScope,
    index: Int,
    collection: PhotoCollection,
    onClick: () -> Unit,
    onUserClick: (User) -> Unit,
    cardWidthPx: Int = calculateCollectionMosaicCardWidthPx(),
    sideWidthPx: Int = cardWidthPx / 3,
    isTransitionSource: Boolean = false,
) {
    val context = LocalPlatformContext.current

    // Use BlurHash from cover photo as card background placeholder with LRU cache
    val blurHash = collection.coverPhoto?.blurHash
    val placeholderPainter = rememberBlurHashPlaceholder(blurHash, BLURHASH_PLACEHOLDER_WIDTH, 18)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .staggeredEntrance(index = index)
                .bounceClick(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mosaic Grid: Left large cover, right two smaller thumbnails
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
            ) {
                // Left Image: Cover Photo (weight 0.62f)
                val coverUrl =
                    remember(collection.id, collection.coverPhoto?.id, collection.coverPhoto?.urls?.raw, cardWidthPx) {
                        collection.coverPhoto
                            ?.urls
                            ?.getOptimalUrl(cardWidthPx * 2 / 3)
                            ?: collection.previewPhotos
                                ?.firstOrNull()
                                ?.urls
                                ?.getOptimalUrl(cardWidthPx * 2 / 3)
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

                val coverPainter =
                    rememberAsyncImagePainter(
                        model = coverRequest,
                        placeholder = placeholderPainter,
                    )

                Box(
                    modifier =
                        Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    if (coverUrl != null) {
                        val coverModifier =
                            if (isTransitionSource && sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier
                                        .fillMaxSize()
                                        .sharedBounds(
                                            sharedContentState =
                                                rememberSharedContentState(
                                                    key = "col_cover_${collection.id}",
                                                ),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                        )
                                }
                            } else {
                                Modifier.fillMaxSize()
                            }

                        Image(
                            painter = coverPainter,
                            contentDescription = collection.title,
                            modifier = coverModifier,
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Column: Two thumbnails (weight 0.38f)
                Column(
                    modifier =
                        Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                ) {
                    // Top Thumbnail
                    val topThumbPhoto =
                        collection.previewPhotos
                            ?.getOrNull(1)
                            ?: collection.previewPhotos
                                ?.getOrNull(0)
                    val topThumbUrl =
                        remember(topThumbPhoto?.id, topThumbPhoto?.urls, sideWidthPx) {
                            topThumbPhoto?.urls?.getOptimalUrl(sideWidthPx)
                        }
                    val topPlaceholderPainter =
                        rememberBlurHashPlaceholder(topThumbPhoto?.blurHash, BLURHASH_PLACEHOLDER_WIDTH / 2, 12)

                    val topThumbRequest =
                        remember(context, topThumbUrl) {
                            topThumbUrl?.let {
                                ImageRequest
                                    .Builder(context)
                                    .data(it)
                                    .crossfade(true)
                                    .build()
                            }
                        }

                    val topThumbPainter =
                        rememberAsyncImagePainter(
                            model = topThumbRequest,
                            placeholder = topPlaceholderPainter,
                        )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        if (topThumbUrl != null) {
                            Image(
                                painter = topThumbPainter,
                                contentDescription = stringResource(R.string.collections_preview_1_accessibility),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Thumbnail
                    val bottomThumbPhoto =
                        collection.previewPhotos
                            ?.getOrNull(2)
                            ?: collection.previewPhotos
                                ?.getOrNull(1)
                    val bottomThumbUrl =
                        remember(bottomThumbPhoto?.id, bottomThumbPhoto?.urls, sideWidthPx) {
                            bottomThumbPhoto?.urls?.getOptimalUrl(sideWidthPx)
                        }
                    val bottomPlaceholderPainter =
                        rememberBlurHashPlaceholder(bottomThumbPhoto?.blurHash, BLURHASH_PLACEHOLDER_WIDTH / 2, 12)

                    val bottomThumbRequest =
                        remember(context, bottomThumbUrl) {
                            bottomThumbUrl?.let {
                                ImageRequest
                                    .Builder(context)
                                    .data(it)
                                    .crossfade(true)
                                    .build()
                            }
                        }

                    val bottomThumbPainter =
                        rememberAsyncImagePainter(
                            model = bottomThumbRequest,
                            placeholder = bottomPlaceholderPainter,
                        )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        if (bottomThumbUrl != null) {
                            Image(
                                painter = bottomThumbPainter,
                                contentDescription = stringResource(R.string.collections_preview_2_accessibility),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            // Collection Information Footer
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val description = collection.description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Curator Profile & Attribution (in-app navigation)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable { onUserClick(collection.user) },
                    ) {
                        val curatorAvatarRequest =
                            remember(context, collection.user.profileImage.small) {
                                ImageRequest
                                    .Builder(context)
                                    .data(collection.user.profileImage.small)
                                    .crossfade(true)
                                    .build()
                            }

                        val avatarPainter =
                            rememberAsyncImagePainter(
                                model = curatorAvatarRequest,
                            )

                        Image(
                            painter = avatarPainter,
                            contentDescription = stringResource(R.string.collections_curator_avatar_accessibility),
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.collections_curated_by),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = collection.user.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Total photos badge
                    Box(
                        modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.collections_photos_count, collection.totalPhotos, collection.totalPhotos),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
