package com.example.imagefeed.android.details

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.imagefeed.android.R
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val ChartDashPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

@Composable
fun StatisticsLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = remember(data) { data.maxOrNull() ?: 1f }
    val minVal = remember(data) { data.minOrNull() ?: 0f }

    Column(modifier = modifier) {
        // Upper reference bound: Max value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.details_metric_peak, formatStatValue(maxVal.toInt())),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.details_trend_30_day),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .drawWithCache {
                        val diff = if (maxVal == minVal) 1f else maxVal - minVal
                        val width = size.width
                        val height = size.height

                        val topY = height * 0.08f
                        val bottomY = height * 0.92f
                        val guidelineColor = outlineVariant.copy(alpha = 0.25f)
                        val guidelineStrokeWidth = 1.dp.toPx()

                        if (data.size == 1) {
                            val circleRadius = 4.dp.toPx()
                            val centerOffset = Offset(width / 2f, height / 2f)
                            onDrawBehind {
                                drawLine(
                                    color = guidelineColor,
                                    start = Offset(0f, topY),
                                    end = Offset(width, topY),
                                    strokeWidth = guidelineStrokeWidth,
                                    pathEffect = ChartDashPathEffect,
                                )
                                drawLine(
                                    color = guidelineColor,
                                    start = Offset(0f, bottomY),
                                    end = Offset(width, bottomY),
                                    strokeWidth = guidelineStrokeWidth,
                                    pathEffect = ChartDashPathEffect,
                                )
                                drawCircle(color = primaryColor, radius = circleRadius, center = centerOffset)
                            }
                        } else {
                            val stepX = width / (data.size - 1).coerceAtLeast(1)
                            val points = ArrayList<Offset>(data.size)
                            for (idx in data.indices) {
                                val value = data[idx]
                                val x = idx * stepX
                                val normalized = if (maxVal == minVal) 0.5f else (value - minVal) / diff
                                val y = topY + (1f - normalized) * (bottomY - topY)
                                points.add(Offset(x, y))
                            }

                            // 1. Draw smooth gradient filling area under cubic Bezier curve
                            val fillPath =
                                Path().apply {
                                    moveTo(points[0].x, height)
                                    lineTo(points[0].x, points[0].y)
                                    for (i in 1 until points.size) {
                                        val prev = points[i - 1]
                                        val curr = points[i]
                                        val midX = (prev.x + curr.x) / 2f
                                        cubicTo(
                                            x1 = midX,
                                            y1 = prev.y,
                                            x2 = midX,
                                            y2 = curr.y,
                                            x3 = curr.x,
                                            y3 = curr.y,
                                        )
                                    }
                                    lineTo(points.last().x, height)
                                    close()
                                }

                            val fillBrush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            primaryColor.copy(alpha = 0.35f),
                                            tertiaryColor.copy(alpha = 0.05f),
                                            Color.Transparent,
                                        ),
                                    startY = 0f,
                                    endY = height,
                                )

                            // 2. Draw cubic Bezier curve with horizontal gradient stroke
                            val strokePath =
                                Path().apply {
                                    moveTo(points[0].x, points[0].y)
                                    for (i in 1 until points.size) {
                                        val prev = points[i - 1]
                                        val curr = points[i]
                                        val midX = (prev.x + curr.x) / 2f
                                        cubicTo(
                                            x1 = midX,
                                            y1 = prev.y,
                                            x2 = midX,
                                            y2 = curr.y,
                                            x3 = curr.x,
                                            y3 = curr.y,
                                        )
                                    }
                                }

                            val strokeBrush =
                                Brush.horizontalGradient(
                                    colors = listOf(primaryColor, tertiaryColor),
                                    startX = 0f,
                                    endX = width,
                                )

                            val strokeStyle = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            val markerOuterRadius = 4.dp.toPx()
                            val markerInnerRadius = 2.dp.toPx()
                            val markerStep = (data.size / 4).coerceAtLeast(1)

                            onDrawBehind {
                                // Reference bounds dashed guidelines
                                drawLine(
                                    color = guidelineColor,
                                    start = Offset(0f, topY),
                                    end = Offset(width, topY),
                                    strokeWidth = guidelineStrokeWidth,
                                    pathEffect = ChartDashPathEffect,
                                )
                                drawLine(
                                    color = guidelineColor,
                                    start = Offset(0f, bottomY),
                                    end = Offset(width, bottomY),
                                    strokeWidth = guidelineStrokeWidth,
                                    pathEffect = ChartDashPathEffect,
                                )

                                drawPath(path = fillPath, brush = fillBrush)
                                drawPath(path = strokePath, brush = strokeBrush, style = strokeStyle)

                                for (idx in points.indices) {
                                    if (idx == 0 || idx == points.size - 1 || idx % markerStep == 0) {
                                        val point = points[idx]
                                        drawCircle(
                                            color = primaryColor,
                                            radius = markerOuterRadius,
                                            center = point,
                                        )
                                        drawCircle(
                                            color = surfaceColor,
                                            radius = markerInnerRadius,
                                            center = point,
                                        )
                                    }
                                }
                            }
                        }
                    },
        )

        Spacer(modifier = Modifier.height(4.dp))
        // Lower reference bound: Min value and latest reading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.details_metric_min, formatStatValue(minVal.toInt())),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            data.lastOrNull()?.let { latestVal ->
                Text(
                    text = stringResource(R.string.details_metric_latest, formatStatValue(latestVal.toInt())),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private val compactDecimalFormatCache =
    ConcurrentHashMap<Locale, ThreadLocal<CompactDecimalFormat>>()

fun formatStatValue(
    value: Int,
    locale: Locale = Locale.getDefault(),
): String {
    val threadLocalFormatter =
        compactDecimalFormatCache.computeIfAbsent(locale) { loc ->
            ThreadLocal.withInitial {
                CompactDecimalFormat.getInstance(
                    loc,
                    CompactDecimalFormat.CompactStyle.SHORT,
                )
            }
        }
    return threadLocalFormatter.get()?.format(value.toLong()) ?: value.toString()
}
