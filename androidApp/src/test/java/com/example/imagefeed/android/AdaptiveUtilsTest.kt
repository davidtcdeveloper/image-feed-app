package com.example.imagefeed.android

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveUtilsTest {
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
