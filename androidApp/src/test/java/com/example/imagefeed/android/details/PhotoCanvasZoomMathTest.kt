@file:Suppress("MagicNumber")

package com.example.imagefeed.android.details

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoCanvasZoomMathTest {
    @Test
    fun clampPanOffsetReturnsZeroAtDefaultScaleOrEmptyContainer() {
        val size = IntSize(width = 1000, height = 1200)
        assertEquals(Offset.Zero, clampPanOffset(Offset(250f, -180f), MIN_ZOOM_SCALE, size))
        assertEquals(Offset.Zero, clampPanOffset(Offset(250f, -180f), 2.5f, IntSize.Zero))
    }

    @Test
    fun clampPanOffsetBoundsWithinScaledCanvasExtents() {
        val size = IntSize(width = 1000, height = 800)
        // At scale 3f: maxX = 1000 * 2 / 2 = 1000, maxY = 800 * 2 / 2 = 800
        val clamped = clampPanOffset(Offset(1500f, -1200f), 3f, size)
        assertEquals(Offset(1000f, -800f), clamped)

        val withinBounds = clampPanOffset(Offset(-300f, 200f), 3f, size)
        assertEquals(Offset(-300f, 200f), withinBounds)
    }

    @Test
    fun calculateDoubleTapTargetZoomsInFromRestAndResetsWhenZoomed() {
        val size = IntSize(width = 1000, height = 1000)

        // Center tap from 1.0x zooms to DOUBLE_TAP_ZOOM_SCALE with zero offset
        val (zoomedScale, centerOffset) =
            calculateDoubleTapTarget(
                currentScale = MIN_ZOOM_SCALE,
                tapOffset = Offset(500f, 500f),
                containerSize = size,
            )
        assertEquals(DOUBLE_TAP_ZOOM_SCALE, zoomedScale, 0.001f)
        assertEquals(Offset.Zero, centerOffset)

        // Corner tap clamps cleanly inside max pan bounds (1000 * 1.5 / 2 = 750)
        val (cornerScale, cornerOffset) =
            calculateDoubleTapTarget(
                currentScale = MIN_ZOOM_SCALE,
                tapOffset = Offset(0f, 0f),
                containerSize = size,
            )
        assertEquals(DOUBLE_TAP_ZOOM_SCALE, cornerScale, 0.001f)
        assertEquals(Offset(750f, 750f), cornerOffset)

        // Double-tap while zoomed resets to 1.0x and Offset.Zero
        val (resetScale, resetOffset) =
            calculateDoubleTapTarget(
                currentScale = DOUBLE_TAP_ZOOM_SCALE,
                tapOffset = Offset(200f, 300f),
                containerSize = size,
            )
        assertEquals(MIN_ZOOM_SCALE, resetScale, 0.001f)
        assertEquals(Offset.Zero, resetOffset)
    }

    @Test
    fun canApplyProgressiveBlurStrictlyEnforcesApi33NormalMotionAndInspectionGates() {
        assertTrue(
            canApplyProgressiveBlur(
                enabled = true,
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isInspection = false,
                reducedMotion = false,
            ),
        )
        assertTrue(
            canApplyProgressiveBlur(
                enabled = true,
                sdkInt = 35,
                isInspection = false,
                reducedMotion = false,
            ),
        )
        assertFalse(
            canApplyProgressiveBlur(
                enabled = true,
                sdkInt = Build.VERSION_CODES.S_V2,
                isInspection = false,
                reducedMotion = false,
            ),
        )
        assertFalse(
            canApplyProgressiveBlur(
                enabled = true,
                sdkInt = Build.VERSION_CODES.Q,
                isInspection = false,
                reducedMotion = false,
            ),
        )
        assertFalse(
            canApplyProgressiveBlur(
                enabled = false,
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isInspection = false,
                reducedMotion = false,
            ),
        )
        assertFalse(
            canApplyProgressiveBlur(
                enabled = true,
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isInspection = true,
                reducedMotion = false,
            ),
        )
        assertFalse(
            canApplyProgressiveBlur(
                enabled = true,
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isInspection = false,
                reducedMotion = true,
            ),
        )
    }

    @Test
    fun calculateTransformTargetAnchorsPinchAroundCentroidAndScalesExistingOffset() {
        val size = IntSize(width = 1000, height = 1000)

        // Pinching 2.5x at top-left corner (0, 0) from 1.0x matches double-tap focal point math
        val corner =
            calculateTransformTarget(
                current = PhotoZoomTransform(),
                centroid = Offset(0f, 0f),
                zoomChange = 2.5f,
                panChange = Offset.Zero,
                containerSize = size,
            )
        assertEquals(2.5f, corner.scale, 0.001f)
        assertEquals(Offset(750f, 750f), corner.offset)

        // Zooming 2x around center while panned scales existing offset proportionally (200 -> 400)
        val scaled =
            calculateTransformTarget(
                current = PhotoZoomTransform(scale = 2f, offset = Offset(200f, -150f)),
                centroid = Offset(500f, 500f),
                zoomChange = 2f,
                panChange = Offset(20f, -10f),
                containerSize = size,
            )
        assertEquals(4f, scaled.scale, 0.001f)
        assertEquals(Offset(420f, -310f), scaled.offset)

        // Zooming back down to 1.0x snaps scale to MIN_ZOOM_SCALE and resets offset to Offset.Zero
        val reset =
            calculateTransformTarget(
                current = PhotoZoomTransform(scale = 2f, offset = Offset(300f, -200f)),
                centroid = Offset(500f, 500f),
                zoomChange = 0.25f,
                panChange = Offset(50f, 50f),
                containerSize = size,
            )
        assertEquals(MIN_ZOOM_SCALE, reset.scale, 0.001f)
        assertEquals(Offset.Zero, reset.offset)
    }

    @Test
    fun interpolateZoomFrameClampsExpressiveSpringOvershoot() {
        val size = IntSize(width = 1000, height = 1000)

        // Zoom-out spring overshoot (fraction = 1.12f) must never shrink below 1.0x or leak offset
        val reset =
            interpolateZoomFrame(
                start = PhotoZoomTransform(scale = DOUBLE_TAP_ZOOM_SCALE, offset = Offset(400f, -300f)),
                target = PhotoZoomTransform(),
                fraction = 1.12f,
                containerSize = size,
            )
        assertEquals(MIN_ZOOM_SCALE, reset.scale, 0.001f)
        assertEquals(Offset.Zero, reset.offset)

        // Zoom-in spring overshoot clamps scale <= MAX_ZOOM_SCALE and keeps pan strictly inside bounds
        val overshoot =
            interpolateZoomFrame(
                start = PhotoZoomTransform(),
                target = PhotoZoomTransform(scale = MAX_ZOOM_SCALE, offset = Offset(1500f, 1500f)),
                fraction = 1.15f,
                containerSize = size,
            )
        assertEquals(MAX_ZOOM_SCALE, overshoot.scale, 0.001f)
        assertEquals(Offset(1500f, 1500f), overshoot.offset)
    }

    @Test
    fun bookmarkMorphProducesNormalizedCircleAndUprightRoundedSquircle() {
        val startCubics = BookmarkMorph.asCubics(0f)
        val midCubics = BookmarkMorph.asCubics(0.5f)
        val endCubics = BookmarkMorph.asCubics(1f)

        assertTrue(startCubics.isNotEmpty())
        assertEquals(startCubics.size, midCubics.size)
        assertEquals(startCubics.size, endCubics.size)

        // At progress = 1f (upright rounded squircle), anchors should reach near corners (e.g. x > 0.8 & y > 0.8),
        // unlike a 45-degree diamond whose max (x + y) around (0.5, 0.5) stays near 1.5.
        val endPoints =
            endCubics.flatMap { cubic ->
                listOf(
                    cubic.anchor0X to cubic.anchor0Y,
                    cubic.anchor1X to cubic.anchor1Y,
                )
            }
        assertTrue(
            endPoints.all { (x, y) -> x in -0.05f..1.05f && y in -0.05f..1.05f },
            "Squircle points should stay normalized inside [0, 1]",
        )
        assertTrue(
            endPoints.any { (x, y) -> x > 0.85f && y > 0.70f },
            "Expected upright axis-aligned rounded squircle corners rather than a 45-degree diamond",
        )
    }
}
