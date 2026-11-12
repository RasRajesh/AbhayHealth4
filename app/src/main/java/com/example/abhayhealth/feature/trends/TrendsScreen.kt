package com.example.abhayhealth.feature.trends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.abhayhealth.R
import com.example.abhayhealth.core.designsystem.components.AbhayHealthBrandLogo
import com.example.abhayhealth.core.designsystem.components.EmptyStateContent
import com.example.abhayhealth.feature.symptoms.AddSymptomDialog
import com.example.abhayhealth.feature.symptoms.SymptomTrendsComponent

@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel,
    modifier: Modifier = Modifier
) {
    val labTrends by viewModel.labTrends.collectAsState()
    val vitalsTrends by viewModel.vitalsTrends.collectAsState()
    val allTrends by viewModel.biomarkerTrends.collectAsState()
    val dualComparison by viewModel.dualComparison.collectAsState()
    val availableMetrics by viewModel.availableComparisonMetrics.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val showAddSymptomDialog by viewModel.showAddSymptomDialog.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        stringResource(id = R.string.trends_tab_labs),
        stringResource(id = R.string.trends_tab_vitals),
        stringResource(id = R.string.trends_tab_compare),
        stringResource(id = R.string.trends_tab_symptoms),
        stringResource(id = R.string.trends_tab_adherence)
    )

    if (showAddSymptomDialog) {
        AddSymptomDialog(
            onDismiss = { viewModel.dismissAddSymptomDialog() },
            onSave = { name, severity, score, notes, triggers, duration ->
                viewModel.logSymptom(name, severity, score, notes, triggers, duration)
            }
        )
    }

    val displayedTrends = when (selectedTabIndex) {
        0 -> labTrends.ifEmpty { allTrends.filter { it.category == "LABS" } }
        1 -> vitalsTrends.ifEmpty { allTrends.filter { it.category == "VITALS" } }
        else -> emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.trends_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.trends_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            // Official AbhayHealth brand launcher logo
            AbhayHealthBrandLogo(
                size = 46.dp,
                elevation = 2.dp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title, maxLines = 1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            2 -> {
                // Dual Metric Comparison Tab (e.g. Weight vs Blood Pressure)
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DualMetricComparisonCard(
                            comparison = dualComparison,
                            availableMetrics = availableMetrics,
                            onSelectMetric1 = { viewModel.setComparisonMetric1(it) },
                            onSelectMetric2 = { viewModel.setComparisonMetric2(it) }
                        )
                    }
                }
            }
            3 -> {
                // Symptom Tracking & Frequency Tab
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SymptomTrendsComponent(
                            symptoms = symptoms,
                            onAddSymptomClick = { viewModel.openAddSymptomDialog() },
                            onDeleteSymptomClick = { viewModel.deleteSymptom(it) }
                        )
                    }
                }
            }
            4 -> {
                // Adherence Tab
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            EmptyStateContent(
                                title = stringResource(id = R.string.trends_empty_adherence_title),
                                description = stringResource(id = R.string.trends_empty_adherence_desc),
                                icon = Icons.AutoMirrored.Filled.TrendingUp
                            )
                        }
                    }
                }
            }
            else -> {
                if (displayedTrends.isEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                when (selectedTabIndex) {
                                    0 -> EmptyStateContent(
                                        title = stringResource(id = R.string.trends_empty_labs_title),
                                        description = stringResource(id = R.string.trends_empty_labs_desc),
                                        icon = Icons.Default.Science
                                    )
                                    else -> EmptyStateContent(
                                        title = stringResource(id = R.string.trends_empty_vitals_title),
                                        description = stringResource(id = R.string.trends_empty_vitals_desc),
                                        icon = Icons.Default.MonitorHeart
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = displayedTrends,
                            key = { it.biomarkerName }
                        ) { trend ->
                            BiomarkerTrendCard(trend = trend)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiomarkerTrendCard(trend: BiomarkerTrend) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trend.biomarkerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!trend.clinicalGuidelineTag.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trend.clinicalGuidelineTag!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${trend.latestValue} ${trend.unit}".trim(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!trend.deltaFromPrevious.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = trend.deltaFromPrevious!!,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!trend.referenceRange.isNullOrBlank()) {
                Text(
                    text = "Ref: ${trend.referenceRange}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Interactive Recharts/D3-style Chart Canvas for historical extracted readings
            val numericPoints = remember(trend.points) {
                trend.points.mapNotNull { pt ->
                    pt.numericValue?.let { num -> Pair(pt, num) }
                }
            }

            if (numericPoints.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                BiomarkerRechartsCanvas(
                    points = numericPoints,
                    unit = trend.unit
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "${trend.points.size} records extracted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun BiomarkerRechartsCanvas(
    points: List<Pair<BiomarkerPoint, Double>>,
    unit: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val values = points.map { it.second }
    val minVal = (values.minOrNull() ?: 0.0).let { if (it > 0.0) it * 0.9 else it }
    val maxVal = (values.maxOrNull() ?: 100.0).let { if (it > 0.0) it * 1.1 else 100.0 }
    val valRange = (maxVal - minVal).coerceAtLeast(1.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        val currentSelected = selectedIndex
        if (currentSelected != null && currentSelected in points.indices) {
            val (pt, value) = points[currentSelected]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "${pt.recordDate}: $value $unit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                        val tappedIdx = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                        selectedIndex = if (selectedIndex == tappedIdx) null else tappedIdx
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val padTop = 16f
            val padBottom = 28f
            val chartH = height - padTop - padBottom
            val n = points.size
            val stepX = width / (n - 1).coerceAtLeast(1)

            // Grid lines (horizontal dotted lines)
            val gridY1 = padTop + chartH * 0.25f
            val gridY2 = padTop + chartH * 0.75f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            drawLine(
                color = outlineColor.copy(alpha = 0.5f),
                start = Offset(0f, gridY1),
                end = Offset(width, gridY1),
                pathEffect = dashEffect,
                strokeWidth = 1.5f
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.5f),
                start = Offset(0f, gridY2),
                end = Offset(width, gridY2),
                pathEffect = dashEffect,
                strokeWidth = 1.5f
            )

            // Compute coordinates
            val coords = points.mapIndexed { idx, pair ->
                val x = idx * stepX
                val normalized = ((pair.second - minVal) / valRange).toFloat().coerceIn(0f, 1f)
                val y = padTop + chartH * (1f - normalized)
                Offset(x, y)
            }

            // Area Gradient Path
            val areaPath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
                lineTo(coords.last().x, padTop + chartH)
                lineTo(coords.first().x, padTop + chartH)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f),
                        primaryColor.copy(alpha = 0.02f)
                    ),
                    startY = padTop,
                    endY = padTop + chartH
                )
            )

            // Stroke Line Path
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }

            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // Draw Dots and Values
            coords.forEachIndexed { i, offset ->
                val isSelected = selectedIndex == i
                // Outer ring
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 10f else 7f,
                    center = offset
                )
                // Colored center
                drawCircle(
                    color = primaryColor,
                    radius = if (isSelected) 7f else 4.5f,
                    center = offset
                )

                // Date label along bottom
                val dateLabel = points[i].first.recordDate
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = onSurfaceColor.hashCode()
                        textSize = 24f
                        isAntiAlias = true
                        textAlign = when (i) {
                            0 -> android.graphics.Paint.Align.LEFT
                            n - 1 -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.CENTER
                        }
                    }
                    drawText(dateLabel, offset.x, height - 6f, paint)
                }
            }
        }
    }
}
