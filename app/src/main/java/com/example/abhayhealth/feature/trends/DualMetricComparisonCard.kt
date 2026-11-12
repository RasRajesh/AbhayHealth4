package com.example.abhayhealth.feature.trends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abhayhealth.R
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DualMetricComparisonCard(
    comparison: DualMetricComparison,
    availableMetrics: List<String>,
    onSelectMetric1: (String) -> Unit,
    onSelectMetric2: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMetric1 by remember { mutableStateOf(false) }
    var expandedMetric2 by remember { mutableStateOf(false) }

    val color1 = Color(0xFF0284C7) // Neon Sky Blue / Primary
    val color2 = Color(0xFFE11D48) // Rose / Crimson / Secondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dual_metric_comparison_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(id = R.string.trends_compare_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.trends_compare_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric Selectors Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1 Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedMetric1 = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, color1.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color1)
                            )
                            Text(
                                text = comparison.metric1Name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color1,
                                maxLines = 1
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expandedMetric1,
                        onDismissRequest = { expandedMetric1 = false }
                    ) {
                        availableMetrics.forEach { metric ->
                            DropdownMenuItem(
                                text = { Text(metric) },
                                onClick = {
                                    onSelectMetric1(metric)
                                    expandedMetric1 = false
                                },
                                leadingIcon = if (metric == comparison.metric1Name) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = color1) }
                                } else null
                            )
                        }
                    }
                }

                // VS separator
                Text(
                    text = "vs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                // Metric 2 Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedMetric2 = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, color2.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color2)
                            )
                            Text(
                                text = comparison.metric2Name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color2,
                                maxLines = 1
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expandedMetric2,
                        onDismissRequest = { expandedMetric2 = false }
                    ) {
                        availableMetrics.forEach { metric ->
                            DropdownMenuItem(
                                text = { Text(metric) },
                                onClick = {
                                    onSelectMetric2(metric)
                                    expandedMetric2 = false
                                },
                                leadingIcon = if (metric == comparison.metric2Name) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = color2) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Recharts/D3 Canvas
            DualAxisRechartsCanvas(
                comparison = comparison,
                color1 = color1,
                color2 = color2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Clinical Cross-Correlation Insight Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cross-Metric Correlation",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = comparison.correlationInsight,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (comparison.isSampleData) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Clinical reference correlation displayed. Logging your daily readings will plot your personalized data.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Recharts/D3-inspired Dual Y-Axis Jetpack Compose Canvas.
 * Plots Metric 1 (Left Y-Axis, Blue) and Metric 2 (Right Y-Axis, Rose)
 * with cubic Bezier curves, dual axes, dotted reference grid lines,
 * and interactive tap inspection.
 */
@Composable
private fun DualAxisRechartsCanvas(
    comparison: DualMetricComparison,
    color1: Color,
    color2: Color
) {
    val points = comparison.points
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    if (points.isEmpty()) return

    // Calculate ranges for Metric 1
    val v1Values = points.map { it.value1 }
    val min1 = (v1Values.minOrNull() ?: 0.0).let { if (it > 0.0) it * 0.92 else it }
    val max1 = (v1Values.maxOrNull() ?: 100.0).let { if (it > 0.0) it * 1.08 else 100.0 }
    val range1 = (max1 - min1).coerceAtLeast(1.0)

    // Calculate ranges for Metric 2
    val v2Values = points.map { it.value2 }
    val min2 = (v2Values.minOrNull() ?: 0.0).let { if (it > 0.0) it * 0.92 else it }
    val max2 = (v2Values.maxOrNull() ?: 100.0).let { if (it > 0.0) it * 1.08 else 100.0 }
    val range2 = (max2 - min2).coerceAtLeast(1.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Inspection tooltip pill when user taps a point
        val curSelected = selectedIndex
        if (curSelected != null && curSelected in points.indices) {
            val pt = points[curSelected]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pt.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${comparison.metric1Name}: ${pt.value1} ${comparison.metric1Unit}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color1
                        )
                        Text(
                            text = "${comparison.metric2Name}: ${pt.value2} ${comparison.metric2Unit}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color2
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val stepX = (size.width - 90f) / (points.size - 1).coerceAtLeast(1)
                        val relativeX = offset.x - 45f
                        val tappedIdx = (relativeX / stepX).roundToInt().coerceIn(0, points.size - 1)
                        selectedIndex = if (selectedIndex == tappedIdx) null else tappedIdx
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val padLeft = 45f
            val padRight = 45f
            val padTop = 18f
            val padBottom = 26f
            val chartW = width - padLeft - padRight
            val chartH = height - padTop - padBottom
            val n = points.size

            val stepX = chartW / (n - 1).coerceAtLeast(1)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // Horizontal grid lines
            val gridY1 = padTop + chartH * 0.25f
            val gridY2 = padTop + chartH * 0.75f
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(padLeft, gridY1),
                end = Offset(width - padRight, gridY1),
                pathEffect = dashEffect,
                strokeWidth = 1.2f
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(padLeft, gridY2),
                end = Offset(width - padRight, gridY2),
                pathEffect = dashEffect,
                strokeWidth = 1.2f
            )

            // Compute Coordinates for Metric 1 (Left Y-Axis)
            val coords1 = points.mapIndexed { i, pt ->
                val x = padLeft + (i * stepX)
                val norm = ((pt.value1 - min1) / range1).toFloat().coerceIn(0f, 1f)
                val y = padTop + chartH * (1f - norm)
                Offset(x, y)
            }

            // Compute Coordinates for Metric 2 (Right Y-Axis)
            val coords2 = points.mapIndexed { i, pt ->
                val x = padLeft + (i * stepX)
                val norm = ((pt.value2 - min2) / range2).toFloat().coerceIn(0f, 1f)
                val y = padTop + chartH * (1f - norm)
                Offset(x, y)
            }

            // Area Gradient for Metric 1
            val areaPath1 = Path().apply {
                moveTo(coords1.first().x, coords1.first().y)
                for (i in 1 until coords1.size) {
                    val prev = coords1[i - 1]
                    val curr = coords1[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
                lineTo(coords1.last().x, padTop + chartH)
                lineTo(coords1.first().x, padTop + chartH)
                close()
            }
            drawPath(
                path = areaPath1,
                brush = Brush.verticalGradient(
                    colors = listOf(color1.copy(alpha = 0.20f), color1.copy(alpha = 0.02f)),
                    startY = padTop,
                    endY = padTop + chartH
                )
            )

            // Line Path for Metric 1
            val linePath1 = Path().apply {
                moveTo(coords1.first().x, coords1.first().y)
                for (i in 1 until coords1.size) {
                    val prev = coords1[i - 1]
                    val curr = coords1[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = linePath1,
                color = color1,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Line Path for Metric 2
            val linePath2 = Path().apply {
                moveTo(coords2.first().x, coords2.first().y)
                for (i in 1 until coords2.size) {
                    val prev = coords2[i - 1]
                    val curr = coords2[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = linePath2,
                color = color2,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Draw Dots & Highlights
            coords1.forEachIndexed { i, offset ->
                val isSelected = selectedIndex == i
                drawCircle(color = Color.White, radius = if (isSelected) 8f else 5.5f, center = offset)
                drawCircle(color = color1, radius = if (isSelected) 6f else 4f, center = offset)
            }

            coords2.forEachIndexed { i, offset ->
                val isSelected = selectedIndex == i
                drawCircle(color = Color.White, radius = if (isSelected) 8f else 5.5f, center = offset)
                drawCircle(color = color2, radius = if (isSelected) 6f else 4f, center = offset)
            }

            // Left Y-Axis Labels (Metric 1)
            drawContext.canvas.nativeCanvas.apply {
                val paint1 = android.graphics.Paint().apply {
                    color = color1.hashCode()
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawText("${max1.roundToInt()}", padLeft - 8f, padTop + 14f, paint1)
                drawText("${min1.roundToInt()}", padLeft - 8f, padTop + chartH, paint1)

                // Right Y-Axis Labels (Metric 2)
                val paint2 = android.graphics.Paint().apply {
                    color = color2.hashCode()
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                drawText("${max2.roundToInt()}", width - padRight + 8f, padTop + 14f, paint2)
                drawText("${min2.roundToInt()}", width - padRight + 8f, padTop + chartH, paint2)

                // X-Axis Date Labels along bottom
                val datePaint = android.graphics.Paint().apply {
                    color = onSurfaceColor.hashCode()
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                points.forEachIndexed { i, pt ->
                    val x = padLeft + (i * stepX)
                    drawText(pt.dateLabel, x, height - 6f, datePaint)
                }
            }
        }
    }
}
