package com.example.imagefeed.android.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagefeed.android.R
import com.example.imagefeed.android.theme.ConnectedToggleGroup
import com.example.imagefeed.presentation.search.SearchFilters

@Suppress("MagicNumber")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    filters: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit,
) {
    var selectedOrderBy by remember { mutableStateOf(filters.orderBy) }
    var selectedOrientation by remember { mutableStateOf(filters.orientation) }
    var selectedColor by remember { mutableStateOf(filters.color) }
    val haptic = LocalHapticFeedback.current

    val anyLabel = stringResource(R.string.search_filter_color_any)
    val bwLabel = stringResource(R.string.search_filter_color_bw)
    val blackLabel = stringResource(R.string.search_filter_color_black)
    val whiteLabel = stringResource(R.string.search_filter_color_white)
    val yellowLabel = stringResource(R.string.search_filter_color_yellow)
    val orangeLabel = stringResource(R.string.search_filter_color_orange)
    val redLabel = stringResource(R.string.search_filter_color_red)
    val purpleLabel = stringResource(R.string.search_filter_color_purple)
    val magentaLabel = stringResource(R.string.search_filter_color_magenta)
    val greenLabel = stringResource(R.string.search_filter_color_green)
    val tealLabel = stringResource(R.string.search_filter_color_teal)
    val blueLabel = stringResource(R.string.search_filter_color_blue)

    val colors =
        remember(
            anyLabel,
            bwLabel,
            blackLabel,
            whiteLabel,
            yellowLabel,
            orangeLabel,
            redLabel,
            purpleLabel,
            magentaLabel,
            greenLabel,
            tealLabel,
            blueLabel,
        ) {
            listOf(
                Pair(anyLabel, null),
                Pair(bwLabel, "black_and_white"),
                Pair(blackLabel, "black"),
                Pair(whiteLabel, "white"),
                Pair(yellowLabel, "yellow"),
                Pair(orangeLabel, "orange"),
                Pair(redLabel, "red"),
                Pair(purpleLabel, "purple"),
                Pair(magentaLabel, "magenta"),
                Pair(greenLabel, "green"),
                Pair(tealLabel, "teal"),
                Pair(blueLabel, "blue"),
            )
        }

    val colorHexes =
        remember {
            mapOf(
                "black_and_white" to Color.Gray,
                "black" to Color.Black,
                "white" to Color.White,
                "yellow" to Color(0xFFFFEB3B),
                "orange" to Color(0xFFFF9800),
                "red" to Color(0xFFF44336),
                "purple" to Color(0xFF9C27B0),
                "magenta" to Color(0xFFE91E63),
                "green" to Color(0xFF4CAF50),
                "teal" to Color(0xFF009688),
                "blue" to Color(0xFF2196F3),
            )
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.search_filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.search_filter_action_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedOrderBy = "relevant"
                            selectedOrientation = null
                            selectedColor = null
                        },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sort Order Section
            Text(
                stringResource(R.string.search_filter_sort_by),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConnectedToggleGroup(
                options =
                    listOf(
                        "relevant" to stringResource(R.string.search_filter_sort_relevant),
                        "latest" to stringResource(R.string.search_filter_sort_latest),
                    ),
                selected = selectedOrderBy,
                onSelect = { orderBy ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedOrderBy = orderBy
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Orientation Section
            Text(
                stringResource(R.string.search_filter_orientation),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConnectedToggleGroup(
                options =
                    listOf(
                        null to stringResource(R.string.search_filter_orientation_all),
                        "landscape" to stringResource(R.string.search_filter_orientation_landscape),
                        "portrait" to stringResource(R.string.search_filter_orientation_portrait),
                    ),
                selected = selectedOrientation,
                onSelect = { orientation ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedOrientation = orientation
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Color Section
            Text(
                stringResource(R.string.search_filter_color_tone),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Retained LazyRow over Row+horizontalScroll to maintain nested scroll
            // gesture dispatch with ModalBottomSheet drag handling.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = colors,
                    key = { it.second ?: "any" },
                ) { pair ->
                    val name = pair.first
                    val value = pair.second

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedColor = value
                            },
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorHexes[value] ?: MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .border(
                                        width = if (selectedColor == value) 2.dp else 1.dp,
                                        color =
                                            if (selectedColor == value) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        shape = CircleShape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selectedColor == value) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.content_desc_selected),
                                    tint = if (value == "white" || value == "yellow") Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onApply(
                        SearchFilters(
                            orderBy = selectedOrderBy,
                            color = selectedColor,
                            orientation = selectedOrientation,
                        ),
                    )
                    onDismiss()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    stringResource(R.string.search_filter_action_apply),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
