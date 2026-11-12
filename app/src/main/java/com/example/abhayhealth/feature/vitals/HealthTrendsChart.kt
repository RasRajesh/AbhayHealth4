package com.example.abhayhealth.feature.vitals

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abhayhealth.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Recharts-inspired Interactive Data Visualization Component for Jetpack Compose.
 * Displays Blood Pressure (dual-line Systolic/Diastolic) or Weight trends from the Room database
 * with smooth cubic Bézier curves, area gradients, horizontal grid lines, reference thresholds,
 * and an interactive touch/scrubber tooltip.
 */
@Composable
fun HealthTrendsChartCard(
    readings: List<VitalReadingEntity>,
    selectedMetric: VitalType,
    onSelectMetric: (VitalType) -> Unit,
    onLogReadingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("health_trends_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Subtitle and Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.vitals_chart_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.vitals_chart_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onLogReadingClick,
                    modifier = Modifier.testTag("trends_log_reading_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.vitals_btn_log),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric Selector Pills (Recharts-inspired filter tabs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMetric == VitalType.BLOOD_PRESSURE,
                    onClick = { onSelectMetric(VitalType.BLOOD_PRESSURE) },
                    label = { Text(stringResource(id = R.string.vitals_metric_bp)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("chip_metric_bp")
                )

                FilterChip(
                    selected = selectedMetric == VitalType.WEIGHT,
                    onClick = { onSelectMetric(VitalType.WEIGHT) },
                    label = { Text(stringResource(id = R.string.vitals_metric_weight)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("chip_metric_weight")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter relevant readings
            val filteredReadings = remember(readings, selectedMetric) {
                readings.filter { it.type == selectedMetric.name }.sortedBy { it.timestamp }
            }

            if (filteredReadings.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.vitals_empty_chart),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Metric Statistics Summary Strip
                VitalSummaryMetricsRow(
                    metricType = selectedMetric,
                    readings = filteredReadings
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Recharts-inspired Interactive Canvas Chart
                AnimatedContent(
                    targetState = selectedMetric,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "chart_transition"
                ) { metric ->
                    RechartsCanvasChart(
                        readings = filteredReadings,
                        metricType = metric
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Legend & Reference Threshold Row
                ChartLegendAndReferenceRow(metricType = selectedMetric)
            }
        }
    }
}

/**
 * Summary Stats bar (Latest, 7-Day Average, Min - Max Range)
 */
@Composable
private fun VitalSummaryMetricsRow(
    metricType: VitalType,
    readings: List<VitalReadingEntity>
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (metricType) {
                VitalType.BLOOD_PRESSURE -> {
                    val latest = readings.lastOrNull()
                    val avgSys = readings.mapNotNull { it.systolic }.average()
                    val avgDia = readings.mapNotNull { it.diastolic }.average()

                    val minSys = readings.mapNotNull { it.systolic }.minOrNull() ?: 0.0
                    val maxSys = readings.mapNotNull { it.systolic }.maxOrNull() ?: 0.0
                    val minDia = readings.mapNotNull { it.diastolic }.minOrNull() ?: 0.0
                    val maxDia = readings.mapNotNull { it.diastolic }.maxOrNull() ?: 0.0

                    MetricStatColumn(
                        label = stringResource(id = R.string.vitals_latest),
                        value = "${latest?.systolic?.roundToInt() ?: "--"}/${latest?.diastolic?.roundToInt() ?: "--"}",
                        unit = stringResource(id = R.string.vitals_unit_bp),
                        highlight = true
                    )
                    MetricStatColumn(
                        label = stringResource(id = R.string.vitals_avg),
                        value = "${avgSys.roundToInt()}/${avgDia.roundToInt()}",
                        unit = stringResource(id = R.string.vitals_unit_bp)
                    )
                    MetricStatColumn(
                        label = stringResource(id = R.string.vitals_range),
                        value = "${minSys.roundToInt()}/${minDia.roundToInt()} – ${maxSys.roundToInt()}/${maxDia.roundToInt()}",
                        unit = ""
                    )
                }
                VitalType.WEIGHT -> {
                    val latest = readings.lastOrNull()?.weightKg
                    val avgWeight = readings.mapNotNull { it.weightKg }.average()
                    val minWeight = readings.mapNotNull { it.weightKg }.minOrNull() ?: 0.0
                    val maxWeight = readings.mapNotNull { it.weightKg }.maxOrNull() ?: 0.0

                    MetricStatColumn(
                        label = stringResource(id = R.string.vitals_latest),
                        value = latest?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                        unit = stringResource(id = R.string.vitals_unit_weight),
                        highlight = true
                    )
                    MetricStatColumn(
                        label = stringResource(id = R.string.vitals_avg),
                        value = String.format(Locale.US, "%.1f", avgWeight),
                        unit = stringResource(id = R.string.vitals_unit_weight)
                    )
                    MetricStatColumn(
                        label = stringResource(id = R.string.vitals_range),
                        value = "${String.format(Locale.US, "%.1f", minWeight)} – ${String.format(Locale.US, "%.1f", maxWeight)}",
                        unit = stringResource(id = R.string.vitals_unit_weight)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricStatColumn(
    label: String,
    value: String,
    unit: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Native Jetpack Compose Canvas rendering a Recharts-inspired Area/Line Chart.
 * Features:
 * - Smooth Cubic Bézier Curves
 * - Area Gradients under curves
 * - Dashed horizontal reference gridlines
 * - Target Reference Lines (e.g. 120 mmHg standard limit)
 * - Interactive Scrubber Tooltip on touch / drag
 */
@Composable
private fun RechartsCanvasChart(
    readings: List<VitalReadingEntity>,
    metricType: VitalType
) {
    var activeTouchIndex by remember(readings) { mutableStateOf<Int?>(null) }

    // Colors matching Material 3 palette
    val systolicColor = MaterialTheme.colorScheme.primary
    val diastolicColor = MaterialTheme.colorScheme.tertiary
    val weightColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val guideLineColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
    val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant

    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(readings) {
                    detectTapGestures(
                        onPress = { offset ->
                            val pointIndex = calculateNearestIndex(offset.x, size.width.toFloat(), readings.size)
                            activeTouchIndex = pointIndex
                        }
                    )
                }
                .pointerInput(readings) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeTouchIndex = calculateNearestIndex(offset.x, size.width.toFloat(), readings.size)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activeTouchIndex = calculateNearestIndex(change.position.x, size.width.toFloat(), readings.size)
                        },
                        onDragEnd = {
                            // keep active or keep showing last point
                        }
                    )
                }
        ) {
            if (readings.isEmpty()) return@Canvas

            val chartPaddingLeft = 40.dp.toPx()
            val chartPaddingRight = 20.dp.toPx()
            val chartPaddingTop = 24.dp.toPx()
            val chartPaddingBottom = 34.dp.toPx()

            val chartWidth = size.width - chartPaddingLeft - chartPaddingRight
            val chartHeight = size.height - chartPaddingTop - chartPaddingBottom

            // Determine data scale min / max
            val (minVal, maxVal) = when (metricType) {
                VitalType.BLOOD_PRESSURE -> {
                    val allVals = readings.flatMap { listOfNotNull(it.systolic, it.diastolic) }
                    val min = (allVals.minOrNull() ?: 60.0) - 10.0
                    val max = (allVals.maxOrNull() ?: 140.0) + 15.0
                    Pair(min.coerceAtLeast(40.0), max.coerceAtLeast(140.0))
                }
                VitalType.WEIGHT -> {
                    val allVals = readings.mapNotNull { it.weightKg }
                    val min = (allVals.minOrNull() ?: 60.0) - 2.0
                    val max = (allVals.maxOrNull() ?: 80.0) + 2.0
                    Pair(min, max)
                }
            }

            val valRange = (maxVal - minVal).coerceAtLeast(1.0)

            fun getY(value: Double): Float {
                val ratio = ((value - minVal) / valRange).toFloat()
                return chartPaddingTop + chartHeight * (1f - ratio)
            }

            fun getX(index: Int): Float {
                return if (readings.size <= 1) {
                    chartPaddingLeft + chartWidth / 2f
                } else {
                    chartPaddingLeft + (index.toFloat() / (readings.size - 1)) * chartWidth
                }
            }

            // 1. Draw Dashed Gridlines (Recharts Grid)
            val gridStepCount = 3
            val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            for (i in 0..gridStepCount) {
                val gridY = chartPaddingTop + (chartHeight / gridStepCount) * i
                val gridVal = maxVal - (valRange / gridStepCount) * i

                drawLine(
                    color = gridColor,
                    start = Offset(chartPaddingLeft, gridY),
                    end = Offset(size.width - chartPaddingRight, gridY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = gridDashEffect
                )

                // Y-Axis label
                val text = "${gridVal.roundToInt()}"
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    chartPaddingLeft - 10.dp.toPx(),
                    gridY + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textPaintColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                )
            }

            // Reference Guideline for Blood Pressure (120 mmHg standard systolic threshold)
            if (metricType == VitalType.BLOOD_PRESSURE && 120.0 in minVal..maxVal) {
                val refY = getY(120.0)
                drawLine(
                    color = guideLineColor,
                    start = Offset(chartPaddingLeft, refY),
                    end = Offset(size.width - chartPaddingRight, refY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "120 Limit",
                    size.width - chartPaddingRight,
                    refY - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = guideLineColor.hashCode()
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                )
            }

            // 2. Draw Curves & Area Gradients
            when (metricType) {
                VitalType.BLOOD_PRESSURE -> {
                    // Systolic curve
                    val systolicPoints = readings.mapIndexedNotNull { index, reading ->
                        reading.systolic?.let { Offset(getX(index), getY(it)) }
                    }
                    if (systolicPoints.isNotEmpty()) {
                        drawSmoothAreaAndLine(
                            points = systolicPoints,
                            baseY = chartPaddingTop + chartHeight,
                            lineColor = systolicColor,
                            gradientColor = systolicColor
                        )
                    }

                    // Diastolic curve
                    val diastolicPoints = readings.mapIndexedNotNull { index, reading ->
                        reading.diastolic?.let { Offset(getX(index), getY(it)) }
                    }
                    if (diastolicPoints.isNotEmpty()) {
                        drawSmoothAreaAndLine(
                            points = diastolicPoints,
                            baseY = chartPaddingTop + chartHeight,
                            lineColor = diastolicColor,
                            gradientColor = diastolicColor
                        )
                    }
                }
                VitalType.WEIGHT -> {
                    val weightPoints = readings.mapIndexedNotNull { index, reading ->
                        reading.weightKg?.let { Offset(getX(index), getY(it)) }
                    }
                    if (weightPoints.isNotEmpty()) {
                        drawSmoothAreaAndLine(
                            points = weightPoints,
                            baseY = chartPaddingTop + chartHeight,
                            lineColor = weightColor,
                            gradientColor = weightColor
                        )
                    }
                }
            }

            // 3. Draw X-Axis Date Labels
            readings.forEachIndexed { index, reading ->
                // Draw labels for first, middle, last, or if small dataset
                val shouldDrawDate = readings.size <= 5 || index == 0 || index == readings.size - 1 || index == readings.size / 2
                if (shouldDrawDate) {
                    val x = getX(index)
                    val dateStr = dateFormatter.format(Date(reading.timestamp))
                    drawContext.canvas.nativeCanvas.drawText(
                        dateStr,
                        x,
                        size.height - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = textPaintColor.hashCode()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    )
                }
            }

            // 4. Scrubber Vertical Cursor and Highlight Dots
            val activeIdx = activeTouchIndex
            if (activeIdx != null && activeIdx in readings.indices) {
                val reading = readings[activeIdx]
                val activeX = getX(activeIdx)

                // Vertical cursor line
                drawLine(
                    color = systolicColor.copy(alpha = 0.5f),
                    start = Offset(activeX, chartPaddingTop),
                    end = Offset(activeX, chartPaddingTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Highlight dots
                when (metricType) {
                    VitalType.BLOOD_PRESSURE -> {
                        reading.systolic?.let {
                            val ptY = getY(it)
                            drawCircle(color = systolicColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(activeX, ptY))
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(activeX, ptY))
                            drawCircle(color = systolicColor, radius = 3.5.dp.toPx(), center = Offset(activeX, ptY))
                        }
                        reading.diastolic?.let {
                            val ptY = getY(it)
                            drawCircle(color = diastolicColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(activeX, ptY))
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(activeX, ptY))
                            drawCircle(color = diastolicColor, radius = 3.5.dp.toPx(), center = Offset(activeX, ptY))
                        }
                    }
                    VitalType.WEIGHT -> {
                        reading.weightKg?.let {
                            val ptY = getY(it)
                            drawCircle(color = weightColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(activeX, ptY))
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(activeX, ptY))
                            drawCircle(color = weightColor, radius = 3.5.dp.toPx(), center = Offset(activeX, ptY))
                        }
                    }
                }
            }
        }

        // 5. Floating Interactive Tooltip Overlay (Recharts <Tooltip />)
        activeTouchIndex?.let { idx ->
            if (idx in readings.indices) {
                val reading = readings[idx]
                RechartsTooltipCard(
                    reading = reading,
                    metricType = metricType,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Recharts Tooltip Bubble
 */
@Composable
private fun RechartsTooltipCard(
    reading: VitalReadingEntity,
    metricType: VitalType,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateFormatter.format(Date(reading.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (metricType) {
                VitalType.BLOOD_PRESSURE -> {
                    val sys = reading.systolic?.roundToInt() ?: 0
                    val dia = reading.diastolic?.roundToInt() ?: 0
                    val statusText = when {
                        sys < 120 && dia < 80 -> "Optimal (<120/80)"
                        sys in 120..129 && dia < 80 -> "Elevated (120-129)"
                        else -> "Stage 1 HTN (≥130/80)"
                    }
                    Text(
                        text = "$sys / $dia mmHg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sys < 120 && dia < 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                VitalType.WEIGHT -> {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", reading.weightKg ?: 0.0)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (reading.notes.isNotBlank()) {
                        Text(
                            text = reading.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws smooth monotone cubic Bézier spline with shaded gradient underfill
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothAreaAndLine(
    points: List<Offset>,
    baseY: Float,
    lineColor: Color,
    gradientColor: Color
) {
    if (points.isEmpty()) return

    val linePath = Path()
    val areaPath = Path()

    linePath.moveTo(points.first().x, points.first().y)
    areaPath.moveTo(points.first().x, baseY)
    areaPath.lineTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

        linePath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p1.x, p1.y
        )
        areaPath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p1.x, p1.y
        )
    }

    areaPath.lineTo(points.last().x, baseY)
    areaPath.close()

    // 1. Draw Gradient Area Fill
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                gradientColor.copy(alpha = 0.28f),
                gradientColor.copy(alpha = 0.02f)
            ),
            startY = points.minOfOrNull { it.y } ?: 0f,
            endY = baseY
        )
    )

    // 2. Draw Smooth Spline Stroke
    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // 3. Draw Small Point Dots
    points.forEach { pt ->
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = pt
        )
        drawCircle(
            color = lineColor,
            radius = 2.dp.toPx(),
            center = pt
        )
    }
}

private fun calculateNearestIndex(touchX: Float, totalWidth: Float, count: Int): Int {
    if (count <= 1) return 0
    val ratio = (touchX / totalWidth).coerceIn(0f, 1f)
    return (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

/**
 * Chart Legend and Reference Note
 */
@Composable
private fun ChartLegendAndReferenceRow(
    metricType: VitalType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (metricType) {
                VitalType.BLOOD_PRESSURE -> {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = stringResource(id = R.string.vitals_legend_systolic)
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = stringResource(id = R.string.vitals_legend_diastolic)
                    )
                }
                VitalType.WEIGHT -> {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = stringResource(id = R.string.vitals_legend_weight)
                    )
                }
            }
        }

        Text(
            text = when (metricType) {
                VitalType.BLOOD_PRESSURE -> stringResource(id = R.string.vitals_ref_bp)
                VitalType.WEIGHT -> stringResource(id = R.string.vitals_ref_weight)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
