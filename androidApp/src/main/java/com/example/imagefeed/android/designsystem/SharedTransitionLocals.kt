package com.example.imagefeed.android.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Bundles Navigation 3 shared transition scopes so screen and card composables can access
 * transition modifiers via [LocalAppSharedTransition] without deep signature threading.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
data class AppSharedTransition(
    val sharedTransitionScope: SharedTransitionScope,
    val animatedVisibilityScope: AnimatedVisibilityScope,
)

val LocalAppSharedTransition = compositionLocalOf<AppSharedTransition?> { null }

/**
 * Coordinates Satin header blur suppression while a returning shared element is occluded
 * behind a top-edge Satin header during Navigation 3 / Predictive Back transitions.
 */
@Stable
class SatinOcclusionController {
    var headerBottomPx: Float by mutableFloatStateOf(0f)
        private set

    var isOccluded: Boolean by mutableStateOf(false)
        private set

    fun updateHeaderBottom(bottomPx: Float) {
        headerBottomPx = bottomPx
    }

    fun updateOcclusion(occluded: Boolean) {
        isOccluded = occluded
    }
}

val LocalSatinOcclusionController = compositionLocalOf<SatinOcclusionController?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProvideAppSharedTransition(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    val transition =
        remember(sharedTransitionScope, animatedVisibilityScope) {
            AppSharedTransition(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    val occlusionController = remember { SatinOcclusionController() }
    CompositionLocalProvider(
        LocalAppSharedTransition provides transition,
        LocalSatinOcclusionController provides occlusionController,
        content = content,
    )
}
