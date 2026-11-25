package com.example.imagefeed.android.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.android.calculateCollectionMosaicCardWidthPx
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.profile.ProfileTab

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

@Composable
fun CollectionRowLayout(
    collection: PhotoCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val cardWidthPx = calculateCollectionMosaicCardWidthPx()
    val coverPhoto = collection.coverPhoto
    val placeholderPainter = rememberBlurHashPlaceholder(coverPhoto?.blurHash, BLURHASH_PLACEHOLDER_WIDTH, 18)
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
            if (coverUrl != null) {
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
