package com.example.imagefeed.android.designsystem

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val occlusionController = LocalSatinOcclusionController.current

    val canApplyRenderEffectBlur =
        enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isInspection &&
            !reducedMotion

    val blurredModifier =
        if (canApplyRenderEffectBlur) {
            this.blur {
                val occluded = occlusionController?.isOccluded == true
                val activeFraction = if (occluded) 0f else blurFractionProvider().coerceIn(0f, 1f)
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

/**
 * Scaffold container for screens adopting the edge-to-edge Satin top header layer.
 */
@Composable
fun SatinHeaderScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(innerPadding)
        }
    }
}
