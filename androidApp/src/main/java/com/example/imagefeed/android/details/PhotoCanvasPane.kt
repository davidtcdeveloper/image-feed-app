package com.example.imagefeed.android.details

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.android.designsystem.LocalAppSharedTransition
import com.example.imagefeed.android.designsystem.SurfaceTokens
import com.example.imagefeed.model.Photo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal const val MIN_ZOOM_SCALE = 1f
internal const val MAX_ZOOM_SCALE = 4f
internal const val DOUBLE_TAP_ZOOM_SCALE = 2.5f
private const val ZOOM_EPSILON = 0.01f
private const val COMPACT_MIN_ASPECT_RATIO = 0.6f
private const val COMPACT_MAX_ASPECT_RATIO = 1.8f
private const val ATTRIBUTION_BLUR_START_FRACTION = 0.72f
private const val DUAL_PANE_SCRIM_ALPHA = 0.78f
private const val COMPACT_SCRIM_MID_STOP = 0.35f
private const val COMPACT_SCRIM_MID_ALPHA = 0.84f
private const val COMPACT_SCRIM_BOTTOM_ALPHA = 0.96f
private val ATTRIBUTION_BLUR_MAX_RADIUS = 14.dp

const val PHOTO_CANVAS_IMAGE_TAG = "photo_canvas_image"

val PhotoZoomScaleKey = SemanticsPropertyKey<Float>("PhotoZoomScale")
var SemanticsPropertyReceiver.photoZoomScale by PhotoZoomScaleKey

internal data class PhotoZoomTransform(
    val scale: Float = MIN_ZOOM_SCALE,
    val offset: Offset = Offset.Zero,
)

internal fun clampPanOffset(
    offset: Offset,
    scale: Float,
    containerSize: IntSize,
): Offset {
    if (scale <= MIN_ZOOM_SCALE + ZOOM_EPSILON || containerSize.width <= 0 || containerSize.height <= 0) {
        return Offset.Zero
    }
    val maxX = (containerSize.width * (scale - MIN_ZOOM_SCALE)) / 2f
    val maxY = (containerSize.height * (scale - MIN_ZOOM_SCALE)) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

internal fun calculateTransformTarget(
    current: PhotoZoomTransform,
    centroid: Offset,
    zoomChange: Float,
    panChange: Offset,
    containerSize: IntSize,
): PhotoZoomTransform {
    val safeCurrentScale = current.scale.coerceAtLeast(MIN_ZOOM_SCALE)
    val nextScale = (safeCurrentScale * zoomChange).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
    if (nextScale <= MIN_ZOOM_SCALE + ZOOM_EPSILON) {
        return PhotoZoomTransform()
    }
    val effectiveZoom = nextScale / safeCurrentScale
    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
    val focalFromCenter = centroid - center
    val rawOffset =
        Offset(
            x = current.offset.x * effectiveZoom + focalFromCenter.x * (1f - effectiveZoom) + panChange.x,
            y = current.offset.y * effectiveZoom + focalFromCenter.y * (1f - effectiveZoom) + panChange.y,
        )
    return PhotoZoomTransform(
        scale = nextScale,
        offset = clampPanOffset(rawOffset, nextScale, containerSize),
    )
}

internal fun interpolateZoomFrame(
    start: PhotoZoomTransform,
    target: PhotoZoomTransform,
    fraction: Float,
    containerSize: IntSize,
): PhotoZoomTransform {
    val nextScale = lerp(start.scale, target.scale, fraction).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
    val nextOffset = clampPanOffset(lerp(start.offset, target.offset, fraction), nextScale, containerSize)
    return PhotoZoomTransform(scale = nextScale, offset = nextOffset)
}

internal fun calculateDoubleTapTarget(
    currentScale: Float,
    tapOffset: Offset,
    containerSize: IntSize,
): PhotoZoomTransform =
    when {
        currentScale > MIN_ZOOM_SCALE + ZOOM_EPSILON -> PhotoZoomTransform()
        containerSize.width <= 0 || containerSize.height <= 0 -> PhotoZoomTransform(scale = DOUBLE_TAP_ZOOM_SCALE)
        else -> {
            val targetScale = DOUBLE_TAP_ZOOM_SCALE
            val centerX = containerSize.width / 2f
            val centerY = containerSize.height / 2f
            val rawOffset =
                Offset(
                    x = (centerX - tapOffset.x) * (targetScale - MIN_ZOOM_SCALE),
                    y = (centerY - tapOffset.y) * (targetScale - MIN_ZOOM_SCALE),
                )
            PhotoZoomTransform(
                scale = targetScale,
                offset = clampPanOffset(rawOffset, targetScale, containerSize),
            )
        }
    }

internal fun canApplyProgressiveBlur(
    enabled: Boolean,
    sdkInt: Int,
    isInspection: Boolean,
    reducedMotion: Boolean,
): Boolean =
    enabled &&
        sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        !isInspection &&
        !reducedMotion

internal fun isReducedMotionActive(context: Context): Boolean =
    try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    } catch (_: Exception) {
        false
    }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhotoCanvasPane(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = LocalAppSharedTransition.current?.sharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = LocalAppSharedTransition.current?.animatedVisibilityScope,
    photo: Photo,
    painter: Painter,
    aspectRatio: Float,
    isDualPane: Boolean,
    onUserClick: (String) -> Unit,
    ambientTint: Color? = null,
    enableAvatarSharedElement: Boolean = true,
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val isReducedMotion = remember(context, isInspection) { isInspection || isReducedMotionActive(context) }
    val coroutineScope = rememberCoroutineScope()
    val zoomAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    var scale by remember(photo.id) { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomAnimJob by remember(photo.id) { mutableStateOf<Job?>(null) }
    val isZoomed by remember(photo.id) { derivedStateOf { scale > MIN_ZOOM_SCALE + ZOOM_EPSILON } }

    DisposableEffect(photo.id) {
        onDispose {
            zoomAnimJob?.cancel()
        }
    }

    val transformState =
        rememberTransformableState { centroid, zoomChange, panChange, _ ->
            zoomAnimJob?.cancel()
            val next =
                calculateTransformTarget(
                    current = PhotoZoomTransform(scale = scale, offset = offset),
                    centroid = centroid,
                    zoomChange = zoomChange,
                    panChange = panChange,
                    containerSize = containerSize,
                )
            scale = next.scale
            offset = next.offset
        }

    val canvasBaseColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val ambientBrush =
        remember(ambientTint, canvasBaseColor) {
            if (ambientTint != null) {
                Brush.verticalGradient(
                    colors = listOf(ambientTint, canvasBaseColor),
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(canvasBaseColor, canvasBaseColor),
                )
            }
        }

    val containerModifier =
        if (isDualPane) {
            modifier.fillMaxSize()
        } else {
            modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceIn(COMPACT_MIN_ASPECT_RATIO, COMPACT_MAX_ASPECT_RATIO))
        }

    val photoSharedElementModifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else {
            Modifier
        }

    Box(
        modifier =
            containerModifier
                .background(ambientBrush)
                .clipToBounds()
                .onSizeChanged { newSize ->
                    containerSize = newSize
                    offset = clampPanOffset(offset, scale, newSize)
                },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = photo.altDescription ?: photo.description,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(PHOTO_CANVAS_IMAGE_TAG)
                    .semantics { photoZoomScale = scale }
                    .pointerInput(photo.id, isReducedMotion) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                zoomAnimJob?.cancel()
                                val start = PhotoZoomTransform(scale = scale, offset = offset)
                                val target = calculateDoubleTapTarget(start.scale, tapOffset, containerSize)
                                if (isReducedMotion) {
                                    scale = target.scale
                                    offset = target.offset
                                } else {
                                    zoomAnimJob =
                                        coroutineScope.launch {
                                            animate(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = zoomAnimationSpec,
                                            ) { fraction, _ ->
                                                val frame =
                                                    interpolateZoomFrame(
                                                        start = start,
                                                        target = target,
                                                        fraction = fraction,
                                                        containerSize = containerSize,
                                                    )
                                                scale = frame.scale
                                                offset = frame.offset
                                            }
                                        }
                                }
                            },
                        )
                    }.transformable(
                        state = transformState,
                        canPan = { scale > MIN_ZOOM_SCALE + ZOOM_EPSILON },
                    ).graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }.attributionBottomProgressiveBlur(enabled = !isZoomed)
                    .then(photoSharedElementModifier),
            contentScale = if (isDualPane) ContentScale.Fit else ContentScale.Crop,
        )

        if (isDualPane) {
            DualPaneAttributionOverlay(
                photo = photo,
                enableAvatarSharedElement = enableAvatarSharedElement,
                onUserClick = onUserClick,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        } else {
            CompactAttributionOverlay(
                photo = photo,
                enableAvatarSharedElement = enableAvatarSharedElement,
                onUserClick = onUserClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DualPaneAttributionOverlay(
    photo: Photo,
    enableAvatarSharedElement: Boolean,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val sharedTransition = LocalAppSharedTransition.current
    val scrimColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, scrimColor.copy(alpha = DUAL_PANE_SCRIM_ALPHA)),
                    ),
                ).padding(16.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = SurfaceTokens.matteScrimPillColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = SurfaceTokens.matteBorder(),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
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
                val baseAvatarModifier = Modifier.size(36.dp)
                val avatarModifier =
                    if (enableAvatarSharedElement && photo.user.username.isNotBlank() && sharedTransition != null) {
                        with(sharedTransition.sharedTransitionScope) {
                            baseAvatarModifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "avatar_${photo.user.username}"),
                                animatedVisibilityScope = sharedTransition.animatedVisibilityScope,
                            )
                        }
                    } else {
                        baseAvatarModifier
                    }

                Image(
                    painter = avatarPainter,
                    contentDescription = stringResource(R.string.content_desc_photographer_avatar),
                    contentScale = ContentScale.Crop,
                    modifier =
                        avatarModifier
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = photo.user.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (photo.user.username.isNotEmpty()) {
                        Text(
                            text = "@${photo.user.username}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.search_user_card_view_profile_accessibility),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CompactAttributionOverlay(
    photo: Photo,
    enableAvatarSharedElement: Boolean,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val sharedTransition = LocalAppSharedTransition.current
    val scrimSurface = MaterialTheme.colorScheme.surface

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        COMPACT_SCRIM_MID_STOP to scrimSurface.copy(alpha = COMPACT_SCRIM_MID_ALPHA),
                        1f to scrimSurface.copy(alpha = COMPACT_SCRIM_BOTTOM_ALPHA),
                    ),
                ).padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
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
            val baseAvatarModifier = Modifier.size(44.dp)
            val avatarModifier =
                if (enableAvatarSharedElement && photo.user.username.isNotBlank() && sharedTransition != null) {
                    with(sharedTransition.sharedTransitionScope) {
                        baseAvatarModifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "avatar_${photo.user.username}"),
                            animatedVisibilityScope = sharedTransition.animatedVisibilityScope,
                        )
                    }
                } else {
                    baseAvatarModifier
                }

            Image(
                painter = avatarPainter,
                contentDescription = stringResource(R.string.content_desc_photographer_avatar),
                contentScale = ContentScale.Crop,
                modifier =
                    avatarModifier
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.user.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (photo.user.username.isNotEmpty()) {
                    Text(
                        text = "@${photo.user.username}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.search_user_card_view_profile_accessibility),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Applies a subtle progressive vertical blur over the bottom attribution region of a photo
 * strictly on API 33+ when reduced motion and inspection mode are inactive, degrading
 * gracefully to the tonal scrim on API 29-32.
 */
@Composable
private fun Modifier.attributionBottomProgressiveBlur(enabled: Boolean = true): Modifier {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val reducedMotion = remember(context) { isReducedMotionActive(context) }
    val shouldBlur =
        canApplyProgressiveBlur(
            enabled = enabled,
            sdkInt = Build.VERSION.SDK_INT,
            isInspection = isInspection,
            reducedMotion = reducedMotion,
        )
    return if (!shouldBlur) {
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
}
