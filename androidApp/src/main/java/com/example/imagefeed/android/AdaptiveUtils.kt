package com.example.imagefeed.android

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import kotlin.math.roundToInt

private const val PANE_WIDTH_QUANTIZE_STEP_DP = 4

val LocalPaneWidthDp = compositionLocalOf<Dp?> { null }

fun quantizePaneWidthDp(
    widthDp: Dp,
    stepDp: Int = PANE_WIDTH_QUANTIZE_STEP_DP,
): Dp {
    val safeStep = stepDp.coerceAtLeast(1)
    val raw = widthDp.value.roundToInt().coerceAtLeast(0)
    return ((raw / safeStep) * safeStep).dp
}

fun resolveEffectiveWidthDp(
    paneWidthDp: Dp?,
    screenWidthDp: Int,
): Int = paneWidthDp?.value?.roundToInt()?.takeIf { it > 0 } ?: screenWidthDp

@Composable
fun ProvidePaneWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val quantizedWidth = remember(maxWidth) { quantizePaneWidthDp(maxWidth) }
        CompositionLocalProvider(LocalPaneWidthDp provides quantizedWidth) {
            content()
        }
    }
}

@Composable
private fun rememberEffectiveWidthDp(): Int {
    val paneWidthDp = LocalPaneWidthDp.current
    val screenWidthDp = LocalAdaptiveLayoutInfo.current.screenWidthDp
    return remember(paneWidthDp, screenWidthDp) {
        resolveEffectiveWidthDp(paneWidthDp, screenWidthDp)
    }
}

private const val WIDTH_EXPANDED_ULTRA_DP = 1200
private const val WIDTH_EXPANDED_LARGE_DP = 840
private const val WIDTH_MEDIUM_DP = 600
private const val WIDTH_COLLECTIONS_EXPANDED_DP = 900

private const val PHOTO_CELL_ULTRA_DP = 240
private const val PHOTO_CELL_LARGE_DP = 220
private const val PHOTO_CELL_MEDIUM_DP = 180
private const val COLLECTION_CELL_EXPANDED_DP = 360
private const val USER_CELL_EXPANDED_DP = 280

private const val MIN_COLS_ULTRA = 5
private const val MIN_COLS_LARGE = 4
private const val MIN_COLS_MEDIUM = 3
private const val COLS_COMPACT = 2

fun computeGridColumns(screenWidthDp: Int): StaggeredGridCells =
    when {
        screenWidthDp >= WIDTH_EXPANDED_ULTRA_DP ->
            StaggeredGridCells.Adaptive(PHOTO_CELL_ULTRA_DP.dp)
        screenWidthDp >= WIDTH_EXPANDED_LARGE_DP ->
            StaggeredGridCells.Adaptive(PHOTO_CELL_LARGE_DP.dp)
        screenWidthDp >= WIDTH_MEDIUM_DP ->
            StaggeredGridCells.Adaptive(PHOTO_CELL_MEDIUM_DP.dp)
        else -> StaggeredGridCells.Fixed(COLS_COMPACT)
    }

fun computeGridColumnCount(screenWidthDp: Int): Int =
    when {
        screenWidthDp >= WIDTH_EXPANDED_ULTRA_DP ->
            (screenWidthDp / PHOTO_CELL_ULTRA_DP).coerceAtLeast(MIN_COLS_ULTRA)
        screenWidthDp >= WIDTH_EXPANDED_LARGE_DP ->
            (screenWidthDp / PHOTO_CELL_LARGE_DP).coerceAtLeast(MIN_COLS_LARGE)
        screenWidthDp >= WIDTH_MEDIUM_DP ->
            (screenWidthDp / PHOTO_CELL_MEDIUM_DP).coerceAtLeast(MIN_COLS_MEDIUM)
        else -> COLS_COMPACT
    }

fun computeCollectionGridColumns(screenWidthDp: Int): StaggeredGridCells =
    when {
        screenWidthDp >= WIDTH_COLLECTIONS_EXPANDED_DP ->
            StaggeredGridCells.Adaptive(COLLECTION_CELL_EXPANDED_DP.dp)
        screenWidthDp >= WIDTH_MEDIUM_DP -> StaggeredGridCells.Fixed(2)
        else -> StaggeredGridCells.Fixed(1)
    }

fun computeUserGridColumns(screenWidthDp: Int): StaggeredGridCells =
    when {
        screenWidthDp >= WIDTH_EXPANDED_LARGE_DP ->
            StaggeredGridCells.Adaptive(USER_CELL_EXPANDED_DP.dp)
        screenWidthDp >= WIDTH_MEDIUM_DP -> StaggeredGridCells.Fixed(2)
        else -> StaggeredGridCells.Fixed(1)
    }

fun computePhotoItemWidthPx(
    screenWidthDp: Int,
    columnCount: Int,
    density: Density,
): Int {
    val itemWidthDp = (screenWidthDp / columnCount.coerceAtLeast(1)).dp
    return with(density) { itemWidthDp.roundToPx() }
}

fun computeCollectionMosaicCardWidthPx(
    screenWidthDp: Int,
    density: Density,
): Int {
    val cardWidthDp = (screenWidthDp - 24).coerceAtLeast(0).dp
    return with(density) { cardWidthDp.roundToPx() }
}

@Composable
fun calculateGridColumns(): StaggeredGridCells {
    val widthDp = rememberEffectiveWidthDp()
    return remember(widthDp) {
        computeGridColumns(widthDp)
    }
}

@Composable
fun calculateGridColumnCount(): Int {
    val widthDp = rememberEffectiveWidthDp()
    return remember(widthDp) {
        computeGridColumnCount(widthDp)
    }
}

@Composable
fun calculateCollectionGridColumns(): StaggeredGridCells {
    val widthDp = rememberEffectiveWidthDp()
    return remember(widthDp) {
        computeCollectionGridColumns(widthDp)
    }
}

@Composable
fun calculateUserGridColumns(): StaggeredGridCells {
    val widthDp = rememberEffectiveWidthDp()
    return remember(widthDp) {
        computeUserGridColumns(widthDp)
    }
}

@Composable
fun calculatePhotoItemWidthPx(columnCount: Int = calculateGridColumnCount()): Int {
    val widthDp = rememberEffectiveWidthDp()
    val density = LocalDensity.current
    return remember(widthDp, columnCount, density) {
        computePhotoItemWidthPx(widthDp, columnCount, density)
    }
}

@Composable
fun calculateCollectionMosaicCardWidthPx(): Int {
    val widthDp = rememberEffectiveWidthDp()
    val density = LocalDensity.current
    return remember(widthDp, density) {
        computeCollectionMosaicCardWidthPx(widthDp, density)
    }
}

internal val MIN_PHONE_PILL_BOTTOM_PADDING = 96.dp
internal val PILL_CONTAINER_BASE_OFFSET = 80.dp
internal val PILL_CONTENT_BREATHING_GAP = 16.dp

fun computeBottomContentPadding(
    useNavigationRail: Boolean,
    navBarBottomInset: Dp,
    defaultPadding: Dp = 12.dp,
): Dp =
    if (useNavigationRail) {
        defaultPadding
    } else {
        max(MIN_PHONE_PILL_BOTTOM_PADDING, PILL_CONTAINER_BASE_OFFSET + navBarBottomInset + PILL_CONTENT_BREATHING_GAP)
    }

@Composable
fun calculateBottomContentPadding(defaultPadding: Dp = 12.dp): Dp {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return computeBottomContentPadding(
        useNavigationRail = adaptiveLayoutInfo.useNavigationRail,
        navBarBottomInset = navBarBottom,
        defaultPadding = defaultPadding,
    )
}
