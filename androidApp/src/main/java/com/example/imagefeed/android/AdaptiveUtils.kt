package com.example.imagefeed.android

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo

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
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    return remember(adaptiveLayoutInfo.screenWidthDp) {
        computeGridColumns(adaptiveLayoutInfo.screenWidthDp)
    }
}

@Composable
fun calculateGridColumnCount(): Int {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    return remember(adaptiveLayoutInfo.screenWidthDp) {
        computeGridColumnCount(adaptiveLayoutInfo.screenWidthDp)
    }
}

@Composable
fun calculateCollectionGridColumns(): StaggeredGridCells {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    return remember(adaptiveLayoutInfo.screenWidthDp) {
        computeCollectionGridColumns(adaptiveLayoutInfo.screenWidthDp)
    }
}

@Composable
fun calculateUserGridColumns(): StaggeredGridCells {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    return remember(adaptiveLayoutInfo.screenWidthDp) {
        computeUserGridColumns(adaptiveLayoutInfo.screenWidthDp)
    }
}

@Composable
fun calculatePhotoItemWidthPx(columnCount: Int = calculateGridColumnCount()): Int {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val density = LocalDensity.current
    return remember(adaptiveLayoutInfo.screenWidthDp, columnCount, density) {
        computePhotoItemWidthPx(adaptiveLayoutInfo.screenWidthDp, columnCount, density)
    }
}

@Composable
fun calculateCollectionMosaicCardWidthPx(): Int {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val density = LocalDensity.current
    return remember(adaptiveLayoutInfo.screenWidthDp, density) {
        computeCollectionMosaicCardWidthPx(adaptiveLayoutInfo.screenWidthDp, density)
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
