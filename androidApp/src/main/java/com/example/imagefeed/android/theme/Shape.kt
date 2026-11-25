package com.example.imagefeed.android.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.example.imagefeed.android.designsystem.ExpressiveShapes
import com.example.imagefeed.android.designsystem.PolygonShape

val Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

private const val APERTURE_REST_CORNER_RADIUS = 0.28f
private const val APERTURE_ACTIVE_INNER_RADIUS = 0.78f
private const val APERTURE_ACTIVE_CORNER_RADIUS = 0.24f
private const val SCALLOP_INNER_RADIUS = 0.84f
private const val SCALLOP_CORNER_RADIUS = 0.45f
private const val SQUIRCLE_CORNER_RADIUS = 0.30f

/**
 * Semantic [RoundedPolygon] and [Shape] tokens reserved strictly for micro-controls,
 * badges, and empty-state backdrops (never for clipping photographs).
 */
object ExpressiveShapeTokens {
    val ApertureRestPolygon: RoundedPolygon =
        RoundedPolygon(
            numVertices = ExpressiveShapes.APERTURE_VERTEX_COUNT,
            rounding = CornerRounding(radius = APERTURE_REST_CORNER_RADIUS),
        ).normalized()

    val ApertureActivePolygon: RoundedPolygon =
        RoundedPolygon
            .star(
                numVerticesPerRadius = ExpressiveShapes.APERTURE_VERTEX_COUNT,
                innerRadius = APERTURE_ACTIVE_INNER_RADIUS,
                rounding = CornerRounding(radius = APERTURE_ACTIVE_CORNER_RADIUS),
            ).normalized()

    val SoftScallopPolygon: RoundedPolygon =
        RoundedPolygon
            .star(
                numVerticesPerRadius = ExpressiveShapes.SCALLOP_VERTEX_COUNT,
                innerRadius = SCALLOP_INNER_RADIUS,
                rounding = CornerRounding(radius = SCALLOP_CORNER_RADIUS),
            ).normalized()

    val SquirclePolygon: RoundedPolygon =
        RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(radius = SQUIRCLE_CORNER_RADIUS),
        ).normalized()

    val ApertureRestShape: Shape = PolygonShape(ApertureRestPolygon)
    val SoftScallopShape: Shape = PolygonShape(SoftScallopPolygon)
    val SquircleShape: Shape = PolygonShape(SquirclePolygon)
}
