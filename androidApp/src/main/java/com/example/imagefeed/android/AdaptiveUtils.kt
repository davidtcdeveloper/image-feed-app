package com.example.imagefeed.android

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun calculateGridColumns(): StaggeredGridCells {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth >= WIDTH_EXPANDED_ULTRA_DP ->
            StaggeredGridCells.Adaptive(PHOTO_CELL_ULTRA_DP.dp)
        screenWidth >= WIDTH_EXPANDED_LARGE_DP ->
            StaggeredGridCells.Adaptive(PHOTO_CELL_LARGE_DP.dp)
        screenWidth >= WIDTH_MEDIUM_DP ->
            StaggeredGridCells.Adaptive(PHOTO_CELL_MEDIUM_DP.dp)
        else -> StaggeredGridCells.Fixed(COLS_COMPACT)
    }
}

@Composable
fun calculateGridColumnCount(): Int {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth >= WIDTH_EXPANDED_ULTRA_DP ->
            (screenWidth / PHOTO_CELL_ULTRA_DP).coerceAtLeast(MIN_COLS_ULTRA)
        screenWidth >= WIDTH_EXPANDED_LARGE_DP ->
            (screenWidth / PHOTO_CELL_LARGE_DP).coerceAtLeast(MIN_COLS_LARGE)
        screenWidth >= WIDTH_MEDIUM_DP ->
            (screenWidth / PHOTO_CELL_MEDIUM_DP).coerceAtLeast(MIN_COLS_MEDIUM)
        else -> COLS_COMPACT
    }
}

@Composable
fun calculateCollectionGridColumns(): StaggeredGridCells {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth >= WIDTH_COLLECTIONS_EXPANDED_DP ->
            StaggeredGridCells.Adaptive(COLLECTION_CELL_EXPANDED_DP.dp)
        screenWidth >= WIDTH_MEDIUM_DP -> StaggeredGridCells.Fixed(2)
        else -> StaggeredGridCells.Fixed(1)
    }
}

@Composable
fun calculateUserGridColumns(): StaggeredGridCells {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth >= WIDTH_EXPANDED_LARGE_DP ->
            StaggeredGridCells.Adaptive(USER_CELL_EXPANDED_DP.dp)
        screenWidth >= WIDTH_MEDIUM_DP -> StaggeredGridCells.Fixed(2)
        else -> StaggeredGridCells.Fixed(1)
    }
}

@Composable
fun calculatePhotoItemWidthPx(columnCount: Int = calculateGridColumnCount()): Int {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val itemWidthDp = (configuration.screenWidthDp / columnCount.coerceAtLeast(1)).dp
    return with(density) { itemWidthDp.roundToPx() }
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
