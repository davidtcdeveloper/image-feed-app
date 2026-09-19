package com.example.imagefeed.android.adaptive

import android.app.Activity
import android.graphics.Rect
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

@Immutable
sealed interface DevicePosture {
    data object Normal : DevicePosture

    data class Book(
        val hingeBounds: Rect,
        val isSeparating: Boolean,
    ) : DevicePosture

    data class TableTop(
        val hingeBounds: Rect,
        val isSeparating: Boolean,
    ) : DevicePosture
}

@Immutable
data class AdaptiveLayoutInfo(
    val windowSizeClass: WindowSizeClass,
    val posture: DevicePosture,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
) {
    val isCompactWidth: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    /**
     * Use navigation rail when width is Medium or Expanded (>= 600dp).
     */
    val useNavigationRail: Boolean
        get() = !isCompactWidth

    /**
     * Use dual-pane layout when on a wide screen or when the foldable device is in
     * Book posture or TableTop posture.
     */
    val useDualPaneDetails: Boolean
        get() = !isCompactWidth || posture is DevicePosture.Book || posture is DevicePosture.TableTop
}

val LocalAdaptiveLayoutInfo =
    compositionLocalOf<AdaptiveLayoutInfo> {
        error("AdaptiveLayoutInfo not provided. Wrap your content with ProvideAdaptiveLayoutInfo.")
    }

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberAdaptiveLayoutInfo(activity: Activity): AdaptiveLayoutInfo {
    val windowSizeClass = calculateWindowSizeClass(activity)
    val configuration = LocalConfiguration.current
    val windowInfoTracker = remember(activity) { WindowInfoTracker.getOrCreate(activity) }

    val layoutInfoState by produceState<WindowLayoutInfo?>(initialValue = null, activity) {
        windowInfoTracker.windowLayoutInfo(activity).collect { value = it }
    }

    val displayFeatures = layoutInfoState?.displayFeatures
    val foldingFeature = displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()
    val posture =
        when {
            foldingFeature == null -> DevicePosture.Normal
            foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL ->
                DevicePosture.Book(
                    hingeBounds = foldingFeature.bounds,
                    isSeparating = foldingFeature.isSeparating,
                )
            foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL ->
                DevicePosture.TableTop(
                    hingeBounds = foldingFeature.bounds,
                    isSeparating = foldingFeature.isSeparating,
                )
            else -> DevicePosture.Normal
        }

    return remember(
        windowSizeClass,
        posture,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        AdaptiveLayoutInfo(
            windowSizeClass = windowSizeClass,
            posture = posture,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
        )
    }
}

@Suppress("FunctionName")
@Composable
fun ProvideAdaptiveLayoutInfo(
    activity: Activity,
    content: @Composable () -> Unit,
) {
    val layoutInfo = rememberAdaptiveLayoutInfo(activity)
    CompositionLocalProvider(LocalAdaptiveLayoutInfo provides layoutInfo) {
        content()
    }
}
