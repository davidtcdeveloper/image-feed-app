package com.example.imagefeed.android.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.android.calculatePhotoItemWidthPx
import com.example.imagefeed.android.designsystem.LocalAppSharedTransition
import com.example.imagefeed.android.designsystem.LocalSatinOcclusionController
import com.example.imagefeed.android.designsystem.SurfaceTokens
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.android.util.bounceClick
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.android.util.staggeredEntrance
import com.example.imagefeed.model.Photo

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoCard(
    sharedTransitionScope: SharedTransitionScope? = LocalAppSharedTransition.current?.sharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = LocalAppSharedTransition.current?.animatedVisibilityScope,
    index: Int,
    photo: Photo,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    itemWidthPx: Int = calculatePhotoItemWidthPx(),
    showAttribution: Boolean = true,
    isTransitionSource: Boolean = false,
) {
    val context = LocalPlatformContext.current
    val aspectRatio = photo.width.toFloat() / photo.height.toFloat()
    val placeholderHeight = BlurHashDecoder.quantizeHeight(BLURHASH_PLACEHOLDER_WIDTH, aspectRatio)

    // Decode BlurHash placeholder in background with LRU cache
    val placeholderPainter =
        rememberBlurHashPlaceholder(
            blurHash = photo.blurHash,
            width = BLURHASH_PLACEHOLDER_WIDTH,
            height = placeholderHeight,
        )

    val occlusionController = LocalSatinOcclusionController.current
    DisposableEffect(isTransitionSource, occlusionController) {
        onDispose { if (isTransitionSource) occlusionController?.clearSource() }
    }

    val photoUrl =
        remember(photo.id, photo.urls.raw, itemWidthPx) {
            photo.urls.getOptimalUrl(itemWidthPx)
        }

    val photoRequest =
        remember(context, photoUrl) {
            ImageRequest
                .Builder(context)
                .data(photoUrl)
                .crossfade(true)
                .build()
        }

    val painter =
        rememberAsyncImagePainter(
            model = photoRequest,
            placeholder = placeholderPainter,
        )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            Modifier
                .fillMaxWidth()
                .staggeredEntrance(index = index)
                .bounceClick(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val baseImageModifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
            val imageModifier =
                if (isTransitionSource && sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        baseImageModifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ).onGloballyPositioned {
                                val top = it.positionInRoot().y
                                occlusionController?.updateSourceBounds(top, top + it.size.height)
                            }
                    }
                } else {
                    baseImageModifier
                }

            Image(
                painter = painter,
                contentDescription = photo.altDescription ?: photo.description,
                modifier = imageModifier,
                contentScale = ContentScale.Crop,
            )

            if (showAttribution) {
                // Compact Matte corner attribution capsule (zero live backdrop blur)
                Surface(
                    shape = CircleShape,
                    color = SurfaceTokens.matteScrimPillColor(),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = SurfaceTokens.matteBorder(),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .clickable { onUserClick() }
                                .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    ) {
                        val avatarRequest =
                            remember(context, photo.user.profileImage.small) {
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.small)
                                    .crossfade(true)
                                    .build()
                            }

                        val userProfileImagePainter =
                            rememberAsyncImagePainter(
                                model = avatarRequest,
                            )

                        val baseAvatarModifier = Modifier.size(24.dp)
                        val avatarModifier =
                            if (isTransitionSource && sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    baseAvatarModifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "avatar_${photo.user.username}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                                }
                            } else {
                                baseAvatarModifier
                            }

                        Image(
                            painter = userProfileImagePainter,
                            contentDescription = stringResource(R.string.feed_user_profile_accessibility),
                            modifier = avatarModifier.clip(CircleShape),
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = photo.user.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
