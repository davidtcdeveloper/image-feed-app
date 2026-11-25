package com.example.imagefeed.android.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Semantic surface & outline constants for the Matte -> Satin -> Float hierarchy.
 */
object SurfaceTokens {
    const val MATTE_BORDER_ALPHA: Float = 0.45f
    const val MATTE_SCRIM_PILL_ALPHA: Float = 0.88f
    const val SATIN_SCRIM_TOP_ALPHA: Float = 0.86f
    const val SATIN_SCRIM_MID_ALPHA: Float = 0.48f

    @Composable
    fun matteBorder(isSelected: Boolean = false): BorderStroke {
        val color =
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = MATTE_BORDER_ALPHA)
            }
        return remember(color, isSelected) {
            BorderStroke(if (isSelected) 2.dp else 1.dp, color)
        }
    }

    @Composable
    fun matteScrimPillColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = MATTE_SCRIM_PILL_ALPHA)
}

/**
 * Opaque, shimmerless Matte surface for cards, inspector panels, and discovery tiles.
 * Uses semantic Material 3 tonal surface tokens with zero live backdrop blur passes.
 */
@Composable
fun MatteSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = SurfaceTokens.matteBorder(),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = border,
        content = content,
    )
}
