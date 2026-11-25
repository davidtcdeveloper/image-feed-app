package com.example.imagefeed.android.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import com.example.imagefeed.android.R
import com.example.imagefeed.android.designsystem.MorphPolygonShape
import com.example.imagefeed.android.theme.ExpressiveShapeTokens

private val SwatchMorph =
    Morph(
        start = RoundedPolygon.circle(numVertices = 4).normalized(),
        end = ExpressiveShapeTokens.SquirclePolygon,
    )

@Suppress("MagicNumber")
@Composable
fun SearchColorDial(
    selectedColor: String?,
    onSelectColor: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val colors =
        remember {
            listOf(
                "black_and_white",
                "black",
                "white",
                "yellow",
                "orange",
                "red",
                "purple",
                "magenta",
                "green",
                "teal",
                "blue",
            )
        }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(colors, key = { it }) { colorKey ->
            val isSelected = selectedColor == colorKey
            val ringBorderWidth by animateDpAsState(
                targetValue = if (isSelected) 2.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_ring",
            )
            val ringPadding by animateDpAsState(
                targetValue = if (isSelected) 3.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_padding",
            )
            // Selected swatch morphs CircleShape <-> rounded squircle.
            val morphProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_morph",
            )
            val swatchShape = MorphPolygonShape(morph = SwatchMorph, percentage = morphProgress)
            val ringColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "color_selection_ring_color",
            )

            val brush =
                remember(colorKey) {
                    when (colorKey) {
                        "black_and_white" ->
                            Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFFF0F0F0)))
                        "black" -> SolidColor(Color.Black)
                        "white" -> SolidColor(Color.White)
                        "yellow" -> SolidColor(Color(0xFFFFEB3B))
                        "orange" -> SolidColor(Color(0xFFFF9800))
                        "red" -> SolidColor(Color(0xFFF44336))
                        "purple" -> SolidColor(Color(0xFF9C27B0))
                        "magenta" -> SolidColor(Color(0xFFE91E63))
                        "green" -> SolidColor(Color(0xFF4CAF50))
                        "teal" -> SolidColor(Color(0xFF009688))
                        "blue" -> SolidColor(Color(0xFF2196F3))
                        else -> SolidColor(Color.Gray)
                    }
                }

            val colorLabel =
                when (colorKey) {
                    "black_and_white" -> stringResource(R.string.search_filter_color_bw)
                    "black" -> stringResource(R.string.search_filter_color_black)
                    "white" -> stringResource(R.string.search_filter_color_white)
                    "yellow" -> stringResource(R.string.search_filter_color_yellow)
                    "orange" -> stringResource(R.string.search_filter_color_orange)
                    "red" -> stringResource(R.string.search_filter_color_red)
                    "purple" -> stringResource(R.string.search_filter_color_purple)
                    "magenta" -> stringResource(R.string.search_filter_color_magenta)
                    "green" -> stringResource(R.string.search_filter_color_green)
                    "teal" -> stringResource(R.string.search_filter_color_teal)
                    "blue" -> stringResource(R.string.search_filter_color_blue)
                    else -> colorKey
                }

            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .semantics {
                            contentDescription = colorLabel
                            role = Role.RadioButton
                            selected = isSelected
                        }.clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val target = if (isSelected) null else colorKey
                            onSelectColor(target)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .border(
                                width = ringBorderWidth,
                                color = ringColor,
                                shape = swatchShape,
                            ).padding(ringPadding)
                            .clip(swatchShape)
                            .then(
                                if (colorKey == "white" && !isSelected) {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                } else {
                                    Modifier
                                },
                            ).background(brush),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (colorKey == "white" || colorKey == "yellow") Color.Black else Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
