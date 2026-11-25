package com.example.imagefeed.android.designsystem

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private val DEFAULT_SATIN_BLUR_RADIUS = 16.dp
private const val DEFAULT_SATIN_END_FRACTION = 0.18f

private fun isReducedMotionActive(context: Context): Boolean =
    try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    } catch (_: Exception) {
        false
    }

/**
 * Applies top-edge Satin progressive blur strictly on API 33+ when reduced motion and inspection
 * mode are inactive, degrading cleanly on API 29-32 to a multi-stop vertical [surfaceColor] scrim.
 */
@Composable
fun Modifier.satinProgressiveBlur(
    headerHeightPx: () -> Float,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    maxBlurRadius: Dp = DEFAULT_SATIN_BLUR_RADIUS,
    blurFractionProvider: () -> Float = { 1f },
    enabled: Boolean = true,
): Modifier {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val reducedMotion = remember(context) { isReducedMotionActive(context) }

    val canApplyRenderEffectBlur =
        enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isInspection &&
            !reducedMotion

    val blurredModifier =
        if (canApplyRenderEffectBlur) {
            this.blur {
                val activeFraction = blurFractionProvider().coerceIn(0f, 1f)
                val totalHeightPx = size.height.toPx()
                val heightFraction =
                    if (totalHeightPx > 0f) {
                        (headerHeightPx() / totalHeightPx).coerceIn(0.02f, DEFAULT_SATIN_END_FRACTION * 2f)
                    } else {
                        DEFAULT_SATIN_END_FRACTION
                    }
                val midFraction = heightFraction * 0.65f
                val effectiveRadius = maxBlurRadius * activeFraction

                blurRadiusSpec =
                    androidx.compose.ui.graphics.blur.BlurRadiusSpec.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.blur.BlurStop(
                                fraction = 0f,
                                radius = effectiveRadius,
                            ),
                            androidx.compose.ui.graphics.blur.BlurStop(
                                fraction = midFraction,
                                radius = effectiveRadius * 0.55f,
                            ),
                            androidx.compose.ui.graphics.blur.BlurStop(
                                fraction = heightFraction,
                                radius = 0.dp,
                            ),
                        ),
                    )
                edgeTreatment = BlurredEdgeTreatment.Rectangle
            }
        } else {
            this
        }

    return blurredModifier.drawWithContent {
        drawContent()
        val scrimHeight = headerHeightPx().coerceAtLeast(0f)
        if (scrimHeight > 0f) {
            val scrimBrush =
                Brush.verticalGradient(
                    0f to surfaceColor.copy(alpha = SurfaceTokens.SATIN_SCRIM_TOP_ALPHA),
                    0.65f to surfaceColor.copy(alpha = SurfaceTokens.SATIN_SCRIM_MID_ALPHA),
                    1f to Color.Transparent,
                    startY = 0f,
                    endY = scrimHeight,
                )
            drawRect(
                brush = scrimBrush,
                size = Size(width = size.width, height = scrimHeight),
            )
        }
    }
}

/** Measured Satin header height (expanded) so edge-to-edge grids can reserve top `contentPadding`. */
val LocalSatinHeaderPadding = compositionLocalOf { 0.dp }

private const val OCCLUSION_FEATHER_MS = 550

/**
 * Scaffold for screens adopting the edge-to-edge Satin top header layer: [content] draws beneath a
 * transparent [topBar] and receives the header height as top padding for `contentPadding`.
 * Blur is suppressed while a returning shared-element source is occluded by the header and feathers
 * back in over [OCCLUSION_FEATHER_MS] once the transition settles.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SatinHeaderScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val controller = LocalSatinOcclusionController.current
    val transition = LocalAppSharedTransition.current
    val headerHeightPx = remember { mutableIntStateOf(0) }
    val maxHeaderHeightPx = remember { mutableIntStateOf(0) }
    val feather = remember { Animatable(1f) }

    LaunchedEffect(controller, transition) {
        if (controller == null || transition == null) return@LaunchedEffect
        snapshotFlow {
            transition.sharedTransitionScope.isTransitionActive && controller.isSourceUnderHeader
        }.distinctUntilChanged()
            .collectLatest { occluded ->
                if (occluded) {
                    feather.snapTo(0f)
                } else {
                    feather.animateTo(1f, tween(OCCLUSION_FEATHER_MS))
                }
            }
    }

    val headerPadding = with(density) { maxHeaderHeightPx.intValue.toDp() }

    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalSatinHeaderPadding provides headerPadding) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .satinProgressiveBlur(
                                headerHeightPx = { headerHeightPx.intValue.toFloat() },
                                blurFractionProvider = { feather.value },
                            ),
                ) {
                    content(
                        PaddingValues(
                            top = headerPadding,
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            val height = coordinates.size.height
                            headerHeightPx.intValue = height
                            if (height > maxHeaderHeightPx.intValue) maxHeaderHeightPx.intValue = height
                            controller?.updateHeaderBottom(coordinates.positionInRoot().y + height)
                        },
            ) {
                topBar()
            }
        }
    }
}

/** [PullToRefreshBox] whose indicator sits below the Satin header instead of beneath it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatinPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    headerPadding: Dp = LocalSatinHeaderPadding.current,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = headerPadding),
            )
        },
        content = content,
    )
}
