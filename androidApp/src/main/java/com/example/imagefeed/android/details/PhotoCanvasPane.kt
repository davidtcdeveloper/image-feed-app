package com.example.imagefeed.android.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.model.Photo

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 4f
private const val ATTRIBUTION_BLUR_START_FRACTION = 0.72f
private val ATTRIBUTION_BLUR_MAX_RADIUS = 14.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoCanvasPane(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photo: Photo,
    painter: Painter,
    aspectRatio: Float,
    isDualPane: Boolean,
    onUserClick: (String) -> Unit,
) {
    val context = LocalPlatformContext.current

    if (isDualPane) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState =
            rememberTransformableState { _, zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                if (scale > MIN_ZOOM_SCALE) {
                    offset += panChange
                } else {
                    offset = Offset.Zero
                }
            }

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            with(sharedTransitionScope) {
                Image(
                    painter = painter,
                    contentDescription = photo.altDescription ?: photo.description,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .transformable(state = transformState)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                            ).attributionBottomProgressiveBlur(enabled = scale <= MIN_ZOOM_SCALE)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                    contentScale = ContentScale.Fit,
                )
            }

            // Floating photographer attribution badge at bottom
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            ),
                        ).padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onUserClick(photo.user.username) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    val avatarPainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.medium)
                                    .crossfade(true)
                                    .build(),
                        )

                    Image(
                        painter = avatarPainter,
                        contentDescription = stringResource(R.string.content_desc_photographer_avatar),
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = photo.user.name,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.search_user_card_view_profile_accessibility),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    } else {
        // Standard compact container
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio.coerceIn(0.6f, 1.8f)),
        ) {
            with(sharedTransitionScope) {
                Image(
                    painter = painter,
                    contentDescription = photo.altDescription ?: photo.description,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .attributionBottomProgressiveBlur()
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                    contentScale = ContentScale.Crop,
                )
            }

            // Photographer Attribution on bottom-overlay
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            ),
                        ).padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUserClick(photo.user.username)
                            },
                ) {
                    val avatarPainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.medium)
                                    .crossfade(true)
                                    .build(),
                        )

                    Image(
                        painter = avatarPainter,
                        contentDescription = stringResource(R.string.content_desc_photographer_avatar),
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.user.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.search_user_card_view_profile_accessibility),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Applies a subtle progressive vertical blur over the bottom attribution region of a photo
 * so foreground attribution controls remain legible without blurring the full image.
 */
private fun Modifier.attributionBottomProgressiveBlur(enabled: Boolean = true): Modifier =
    if (!enabled) {
        this
    } else {
        blur {
            blurRadiusSpec =
                BlurRadiusSpec.verticalGradient(
                    listOf(
                        BlurStop(fraction = ATTRIBUTION_BLUR_START_FRACTION, radius = 0.dp),
                        BlurStop(fraction = 1f, radius = ATTRIBUTION_BLUR_MAX_RADIUS),
                    ),
                )
            edgeTreatment = BlurredEdgeTreatment.Rectangle
        }
    }
