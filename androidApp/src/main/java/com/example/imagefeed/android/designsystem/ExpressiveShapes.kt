package com.example.imagefeed.android.designsystem

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.example.imagefeed.android.R
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.theme.ExpressiveShapeTokens

/**
 * Expressive shape constants and micro-interaction primitives (Spec 84).
 *
 * Strictly reserved for controls, badges, and empty-state backdrops; photographs
 * must never be clipped into polygons.
 */
object ExpressiveShapes {
    const val APERTURE_VERTEX_COUNT: Int = 8
    const val SCALLOP_VERTEX_COUNT: Int = 10
    val ApertureContainerSize = 42.dp
    val ApertureIconSize = 20.dp
    const val APERTURE_ROTATION_DURATION_MS = 1400
    const val APERTURE_MORPH_PULSE_DURATION_MS = 650
}

private const val HALF_TURN_DEGREES = 180f
private const val FULL_TURN_DEGREES = 360f
private const val POLYGON_CENTER_OFFSET = 0.5f

private fun buildTransformedPolygonOutline(
    path: Path,
    size: Size,
    rotationDegrees: Float,
): Outline {
    val matrix =
        Matrix().apply {
            scale(x = size.width, y = size.height)
            if (rotationDegrees != 0f) {
                translate(x = POLYGON_CENTER_OFFSET, y = POLYGON_CENTER_OFFSET)
                rotateZ(rotationDegrees)
                translate(x = -POLYGON_CENTER_OFFSET, y = -POLYGON_CENTER_OFFSET)
            }
        }
    path.transform(matrix)
    path.translate(size.center - path.getBounds().center)
    return Outline.Generic(path)
}

/**
 * [Shape] backed by a normalized [RoundedPolygon] scaled and centered to the layout bounds.
 */
class PolygonShape(
    polygon: RoundedPolygon,
    private val rotationDegrees: Float = 0f,
) : Shape {
    private val normalizedPolygon = polygon.normalized()
    private val basePath: Path by lazy {
        android.graphics
            .Path()
            .also { path -> normalizedPolygon.toPath(path) }
            .asComposePath()
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect.Zero)
        }
        val path = Path().apply { addPath(basePath) }
        return buildTransformedPolygonOutline(
            path = path,
            size = size,
            rotationDegrees = rotationDegrees,
        )
    }
}

/**
 * [Shape] that interpolates smoothly between two normalized [RoundedPolygon]s using [Morph].
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotationDegrees: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect.Zero)
        }
        val clampedProgress = percentage.coerceIn(0f, 1f)
        val path = morph.toPath(progress = clampedProgress).asComposePath()
        return buildTransformedPolygonOutline(
            path = path,
            size = size,
            rotationDegrees = rotationDegrees,
        )
    }
}

@Composable
private fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    return remember(context, isInspection) {
        if (isInspection) {
            true
        } else {
            try {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            } catch (_: Exception) {
                false
            }
        }
    }
}

private class FloatHolder(
    var value: Float = 0f,
)

@Composable
private fun rememberApertureMorphProgressProvider(
    isLoading: Boolean,
    isReducedMotion: Boolean,
    settleSpec: AnimationSpec<Float>,
): () -> Float {
    val lastMorph = remember { FloatHolder(0f) }
    val settleMorph = remember { Animatable(0f) }

    if (isLoading && !isReducedMotion) {
        val transition = rememberInfiniteTransition(label = "apertureMorphTransition")
        val animatedMorph =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = ExpressiveShapes.APERTURE_MORPH_PULSE_DURATION_MS,
                                easing = LinearEasing,
                            ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "apertureMorphProgress",
            )
        return remember(animatedMorph, lastMorph) {
            {
                val value = animatedMorph.value
                lastMorph.value = value
                value
            }
        }
    }

    LaunchedEffect(isLoading, isReducedMotion) {
        if (isReducedMotion || lastMorph.value == 0f) {
            settleMorph.snapTo(0f)
            lastMorph.value = 0f
        } else {
            settleMorph.snapTo(lastMorph.value)
            settleMorph.animateTo(targetValue = 0f, animationSpec = settleSpec)
            lastMorph.value = 0f
        }
    }
    return remember(settleMorph) { { settleMorph.value } }
}

@Composable
private fun rememberApertureRotationProvider(
    isLoading: Boolean,
    isReducedMotion: Boolean,
    settleSpec: AnimationSpec<Float>,
): () -> Float {
    val lastAngle = remember { FloatHolder(0f) }
    val settleRotation = remember { Animatable(0f) }

    if (isLoading && !isReducedMotion) {
        val transition = rememberInfiniteTransition(label = "apertureRotationTransition")
        val animatedRotation =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = FULL_TURN_DEGREES,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = ExpressiveShapes.APERTURE_ROTATION_DURATION_MS,
                                easing = LinearEasing,
                            ),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "apertureRotationDegrees",
            )
        return remember(animatedRotation, lastAngle) {
            {
                val value = animatedRotation.value
                lastAngle.value = value
                value
            }
        }
    }

    LaunchedEffect(isLoading, isReducedMotion) {
        val normalizedAngle = lastAngle.value % FULL_TURN_DEGREES
        if (isReducedMotion || normalizedAngle == 0f) {
            settleRotation.snapTo(0f)
            lastAngle.value = 0f
        } else {
            settleRotation.snapTo(normalizedAngle)
            val nearestZero =
                if (normalizedAngle > HALF_TURN_DEGREES) FULL_TURN_DEGREES else 0f
            settleRotation.animateTo(targetValue = nearestZero, animationSpec = settleSpec)
            settleRotation.snapTo(0f)
            lastAngle.value = 0f
        }
    }
    return remember(settleRotation) { { settleRotation.value } }
}

/**
 * 8-sided camera-aperture randomize button that morphs and rotates smoothly while
 * random photo discovery is loading, respecting system reduced-motion preferences.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApertureRandomizeButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberReducedMotionEnabled()
    val morph =
        remember {
            Morph(
                start = ExpressiveShapeTokens.ApertureRestPolygon,
                end = ExpressiveShapeTokens.ApertureActivePolygon,
            )
        }
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val morphProgressProvider =
        rememberApertureMorphProgressProvider(isLoading, isReducedMotion, settleSpec)
    val rotationProvider =
        rememberApertureRotationProvider(isLoading, isReducedMotion, settleSpec)
    val containerColor by animateColorAsState(
        targetValue =
            if (isLoading) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "apertureContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isLoading) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "apertureContentColor",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val loadingDescription = stringResource(R.string.feed_shake_loading)
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .semantics {
                    if (isLoading) stateDescription = loadingDescription
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ExpressiveShapes.ApertureContainerSize)
                    .graphicsLayer {
                        shape =
                            MorphPolygonShape(
                                morph = morph,
                                percentage = morphProgressProvider(),
                                rotationDegrees = rotationProvider(),
                            )
                        clip = true
                    }.background(containerColor)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.feed_randomize_accessibility),
                tint = contentColor,
                modifier = Modifier.size(ExpressiveShapes.ApertureIconSize),
            )
        }
    }
}

@Preview(showBackground = true, name = "Aperture Randomize - Rest")
@Composable
fun ApertureRandomizeButtonRestPreview() {
    PreviewScaffold(widthDp = 72, heightDp = 72, darkTheme = false) { _, _ ->
        Surface(
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                ApertureRandomizeButton(isLoading = false, onClick = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "Aperture Randomize - Loading")
@Composable
fun ApertureRandomizeButtonLoadingPreview() {
    PreviewScaffold(widthDp = 72, heightDp = 72, darkTheme = true) { _, _ ->
        Surface(
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                ApertureRandomizeButton(isLoading = true, onClick = {})
            }
        }
    }
}
