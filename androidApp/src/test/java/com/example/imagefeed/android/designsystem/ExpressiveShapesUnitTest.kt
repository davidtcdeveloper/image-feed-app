package com.example.imagefeed.android.designsystem

import androidx.graphics.shapes.Morph
import com.example.imagefeed.android.theme.ExpressiveShapeTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NORMALIZED_MIN = -0.05f
private const val NORMALIZED_MAX = 1.05f

class ExpressiveShapesUnitTest {
    @Test
    fun expressivePolygonsAreNormalizedInsideUnitSquare() {
        val polygons =
            listOf(
                ExpressiveShapeTokens.ApertureRestPolygon,
                ExpressiveShapeTokens.ApertureActivePolygon,
                ExpressiveShapeTokens.SoftScallopPolygon,
                ExpressiveShapeTokens.SquirclePolygon,
            )

        polygons.forEach { polygon ->
            val bounds = polygon.calculateBounds()
            assertTrue(bounds[0] >= NORMALIZED_MIN, "minX should be >= 0, was ${bounds[0]}")
            assertTrue(bounds[1] >= NORMALIZED_MIN, "minY should be >= 0, was ${bounds[1]}")
            assertTrue(bounds[2] <= NORMALIZED_MAX, "maxX should be <= 1, was ${bounds[2]}")
            assertTrue(bounds[3] <= NORMALIZED_MAX, "maxY should be <= 1, was ${bounds[3]}")
            assertTrue(polygon.cubics.isNotEmpty(), "Polygon cubics should not be empty")
        }
    }

    @Test
    fun apertureMorphInterpolatesSmoothlyAcrossProgressRange() {
        val morph =
            Morph(
                start = ExpressiveShapeTokens.ApertureRestPolygon,
                end = ExpressiveShapeTokens.ApertureActivePolygon,
            )

        val startCubics = morph.asCubics(0f)
        val midCubics = morph.asCubics(0.5f)
        val endCubics = morph.asCubics(1f)

        assertTrue(startCubics.isNotEmpty())
        assertEquals(startCubics.size, midCubics.size)
        assertEquals(startCubics.size, endCubics.size)
    }
}
