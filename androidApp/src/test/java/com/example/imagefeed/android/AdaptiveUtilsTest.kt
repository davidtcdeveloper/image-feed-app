package com.example.imagefeed.android

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveUtilsTest {
    @Test
    fun quantizePaneWidthDpRoundsDownToStepAndClampsNegativeValues() {
        assertEquals(0.dp, quantizePaneWidthDp((-12).dp))
        assertEquals(448.dp, quantizePaneWidthDp(450.dp))
        assertEquals(600.dp, quantizePaneWidthDp(600.dp))
        assertEquals(840.dp, quantizePaneWidthDp(841.dp))
    }

    @Test
    fun resolveEffectiveWidthDpPrefersPaneWidthOverFullScreenWidth() {
        assertEquals(448, resolveEffectiveWidthDp(paneWidthDp = 448.dp, screenWidthDp = 1024))
        assertEquals(1024, resolveEffectiveWidthDp(paneWidthDp = null, screenWidthDp = 1024))
        assertEquals(1024, resolveEffectiveWidthDp(paneWidthDp = 0.dp, screenWidthDp = 1024))
    }

    @Test
    fun narrowListPaneInsideWideWindowAdaptsColumnsAndCdnWidthToPaneWidth() {
        val fullWindowWidthDp = 1024
        val listPaneWidthDp = resolveEffectiveWidthDp(paneWidthDp = 448.dp, screenWidthDp = fullWindowWidthDp)
        val density = Density(2f)

        val columnCount = computeGridColumnCount(listPaneWidthDp)
        assertEquals(2, columnCount)
        assertEquals(StaggeredGridCells.Fixed(2), computeGridColumns(listPaneWidthDp))
        assertEquals(StaggeredGridCells.Fixed(1), computeCollectionGridColumns(listPaneWidthDp))
        assertEquals(StaggeredGridCells.Fixed(1), computeUserGridColumns(listPaneWidthDp))

        // Full window would have yielded 4+ columns and smaller or differently scaled item widths
        assertEquals(448, computePhotoItemWidthPx(listPaneWidthDp, columnCount, density))
        assertEquals(848, computeCollectionMosaicCardWidthPx(listPaneWidthDp, density))
    }

    @Test
    fun computeBottomContentPaddingReturnsDefaultPaddingWhenNavigationRailIsActive() {
        val result =
            computeBottomContentPadding(
                useNavigationRail = true,
                navBarBottomInset = 24.dp,
                defaultPadding = 12.dp,
            )
        assertEquals(12.dp, result)

        val customResult =
            computeBottomContentPadding(
                useNavigationRail = true,
                navBarBottomInset = 48.dp,
                defaultPadding = 8.dp,
            )
        assertEquals(8.dp, customResult)
    }

    @Test
    fun computeBottomContentPaddingCalculatesClearanceForFloatingPillOnPhones() {
        // Zero insets: clamped to MIN_PHONE_PILL_BOTTOM_PADDING (96.dp)
        val zeroInsetResult =
            computeBottomContentPadding(
                useNavigationRail = false,
                navBarBottomInset = 0.dp,
                defaultPadding = 12.dp,
            )
        assertEquals(96.dp, zeroInsetResult)

        // Gesture navigation (24dp insets): 80dp base offset + 24dp insets + 16dp breathing gap = 120dp
        val gestureResult =
            computeBottomContentPadding(
                useNavigationRail = false,
                navBarBottomInset = 24.dp,
                defaultPadding = 8.dp,
            )
        assertEquals(120.dp, gestureResult)

        // Legacy 3-button navigation (48dp insets): 80dp base offset + 48dp insets + 16dp breathing gap = 144dp
        val threeButtonResult =
            computeBottomContentPadding(
                useNavigationRail = false,
                navBarBottomInset = 48.dp,
                defaultPadding = 16.dp,
            )
        assertEquals(144.dp, threeButtonResult)
    }
}
