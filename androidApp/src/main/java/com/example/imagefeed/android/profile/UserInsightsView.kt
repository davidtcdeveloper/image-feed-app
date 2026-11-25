package com.example.imagefeed.android.profile

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagefeed.android.R
import com.example.imagefeed.android.details.StatItem
import com.example.imagefeed.android.details.formatStatValue
import com.example.imagefeed.android.theme.ExpressiveLoadingIndicator
import com.example.imagefeed.model.AppError
import com.example.imagefeed.profile.model.UserStats
import kotlin.math.roundToInt

private val ChartDateFormatter: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter
        .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)

@Composable
fun InsightsTabContent(
    stats: UserStats?,
    isLoading: Boolean,
    error: AppError?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && stats == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                ExpressiveLoadingIndicator()
            }
        } else if (error != null && stats == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.profile_insights_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (stats != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                // Headline Consolidated Stats Panel
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.profile_stat_total_views),
                        value = formatStatValue(stats.views.total),
                        icon = Icons.Default.Visibility,
                        valueStyle = MaterialTheme.typography.titleMedium,
                        labelStyle = MaterialTheme.typography.bodySmall,
                    )
                    StatItem(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.profile_stat_total_downloads),
                        value = formatStatValue(stats.downloads.total),
                        icon = Icons.Default.FileDownload,
                        valueStyle = MaterialTheme.typography.titleMedium,
                        labelStyle = MaterialTheme.typography.bodySmall,
                    )
                }

                // Interactive Views Chart
                stats.views.historical?.values?.let { viewsList ->
                    if (viewsList.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.profile_chart_views_trend),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InteractiveTimelineChart(
                                    data = viewsList.map { it.value.toFloat() },
                                    dates = viewsList.map { it.date },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Downloads Chart
                stats.downloads.historical?.values?.let { downloadsList ->
                    if (downloadsList.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.profile_chart_downloads_trend),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InteractiveTimelineChart(
                                    data = downloadsList.map { it.value.toFloat() },
                                    dates = downloadsList.map { it.date },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveTimelineChart(
    data: List<Float>,
    dates: List<String>,
    modifier: Modifier = Modifier,
) {
    var dragX by remember { mutableStateOf<Float?>(null) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val chartColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = remember(data) { data.maxOrNull() ?: 1f }
    val minVal = remember(data) { data.minOrNull() ?: 0f }
    val diff = remember(maxVal, minVal) { if (maxVal == minVal) 1f else maxVal - minVal }

    val hoveredIndex by remember(data.size) {
        derivedStateOf {
            val currentDragX = dragX
            if (currentDragX != null && data.isNotEmpty() && canvasWidth > 0f) {
                val stepX = canvasWidth / (data.size - 1).coerceAtLeast(1)
                (currentDragX / stepX).roundToInt().coerceIn(0, data.size - 1)
            } else {
                null
            }
        }
    }

    Column(modifier = modifier) {
        // Overlay displaying hovered detail
        val currentHoverIndex = hoveredIndex
        if (currentHoverIndex != null && data.isNotEmpty()) {
            val index = currentHoverIndex
            val formattedDate =
                remember(dates, index) {
                    val dateStr = dates.getOrNull(index) ?: ""
                    try {
                        java.time.LocalDate
                            .parse(dateStr)
                            .format(ChartDateFormatter)
                    } catch (_: Exception) {
                        dateStr
                    }
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formattedDate,
                    color = onSurfaceVariantColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.profile_chart_units, data[index].toInt()),
                    color = onSurfaceColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                )
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_chart_drag_prompt),
                    color = onSurfaceVariantColor,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = stringResource(R.string.details_metric_peak, formatStatValue(maxVal.toInt())),
                    color = onSurfaceColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        Spacer(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { size ->
                        canvasWidth = size.width.toFloat()
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragX = offset.x
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragX = change.position.x
                            },
                            onDragEnd = {
                                dragX = null
                            },
                            onDragCancel = {
                                dragX = null
                            },
                        )
                    }.drawWithCache {
                        val width = size.width
                        val height = size.height

                        if (data.isEmpty()) {
                            onDrawBehind { }
                        } else {
                            val stepX = width / (data.size - 1).coerceAtLeast(1)
                            val points = ArrayList<Offset>(data.size)
                            for (idx in data.indices) {
                                val value = data[idx]
                                val x = idx * stepX
                                val y = height - ((value - minVal) / diff) * height * 0.85f - (height * 0.05f)
                                points.add(Offset(x, y))
                            }

                            // Fill gradient
                            val fillPath =
                                Path().apply {
                                    moveTo(0f, height)
                                    for (i in points.indices) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                    lineTo(width, height)
                                    close()
                                }

                            val fillBrush =
                                Brush.verticalGradient(
                                    colors = listOf(chartColor.copy(alpha = 0.22f), Color.Transparent),
                                    startY = 0f,
                                    endY = height,
                                )

                            // Main stroke
                            val strokePath =
                                Path().apply {
                                    for (idx in points.indices) {
                                        val point = points[idx]
                                        if (idx == 0) {
                                            moveTo(point.x, point.y)
                                        } else {
                                            lineTo(point.x, point.y)
                                        }
                                    }
                                }

                            val strokeWidth = 3.dp.toPx()
                            val indicatorLineWidth = 1.dp.toPx()
                            val outerRadius = 7.dp.toPx()
                            val innerRadius = 3.dp.toPx()
                            val indicatorColor = chartColor.copy(alpha = 0.4f)

                            onDrawBehind {
                                // Draw fill gradient
                                drawPath(
                                    path = fillPath,
                                    brush = fillBrush,
                                )

                                // Draw stroke path
                                drawPath(
                                    path = strokePath,
                                    color = chartColor,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                )

                                // Draw interaction slider line if dragging
                                val currentDragX = dragX
                                if (currentDragX != null) {
                                    val index = (currentDragX / stepX).roundToInt().coerceIn(0, data.size - 1)
                                    val hoverPoint = points[index]

                                    // Draw vertical indicator line
                                    drawLine(
                                        color = indicatorColor,
                                        start = Offset(hoverPoint.x, 0f),
                                        end = Offset(hoverPoint.x, height),
                                        strokeWidth = indicatorLineWidth,
                                    )

                                    // Highlighted interaction dot
                                    drawCircle(
                                        color = chartColor,
                                        radius = outerRadius,
                                        center = hoverPoint,
                                    )
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = innerRadius,
                                        center = hoverPoint,
                                    )
                                }
                            }
                        }
                    },
        )
    }
}
