package com.example.abhayhealth.feature.medicines

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/**
 * Filter period for the Daily Medication Timeline view.
 */
enum class TimelinePeriodFilter(val title: String, val startHour: Float, val endHour: Float) {
    ALL("All Day", 0f, 24f),
    MORNING("Morning", 6f, 12f),
    AFTERNOON("Afternoon", 12f, 17f),
    EVENING("Evening", 17f, 21f),
    NIGHT("Night", 21f, 24f)
}

data class TimelineDoseItem(
    val id: String,
    val name: String,
    val dosage: String,
    val timeSlot: String,
    val isTaken: Boolean
)

/**
 * Daily Timeline View for PrescriptionMedicationEntity (used in HomeScreen).
 */
@Composable
fun MedicationDailyTimelineGraph(
    medications: List<PrescriptionMedicationEntity>,
    onToggleTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = medications.map {
        TimelineDoseItem(it.id, it.name, it.dosage, it.timeSlot, it.isTaken)
    }
    MedicationTimelineGraphInternal(items, onToggleTaken, modifier)
}

/**
 * Daily Timeline View for PrescriptionMedication (used in MedicinesScreen).
 */
@JvmName("MedicationDailyTimelineGraphForModel")
@Composable
fun MedicationDailyTimelineGraph(
    medicationList: List<PrescriptionMedication>,
    onToggleTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = medicationList.map {
        TimelineDoseItem(it.id, it.name, it.dosage, it.timeSlot, it.isTaken)
    }
    MedicationTimelineGraphInternal(items, onToggleTaken, modifier)
}

@Composable
private fun MedicationTimelineGraphInternal(
    medications: List<TimelineDoseItem>,
    onToggleTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(TimelinePeriodFilter.ALL) }

    // Calculate current time in decimal hours (e.g. 14.5 for 2:30 PM)
    val now = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val successColor = Color(0xFF10B981)

    val takenCount = medications.count { it.isTaken }
    val totalCount = medications.size

    // Pulsing animation for upcoming active dose node
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medication_daily_timeline_card"),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title + Progress badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Dose Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalCount > 0) "$takenCount of $totalCount doses taken today" else "24-hour scheduled medication map",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (totalCount > 0 && takenCount == totalCount) {
                        Color(0xFFDCFCE7)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = if (totalCount > 0 && takenCount == totalCount) "All Done ✓" else "Now: ${formatTimeDec(now)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalCount > 0 && takenCount == totalCount) {
                            Color(0xFF166534)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Period Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelinePeriodFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.title, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Custom Compose Canvas Graph (Recharts-inspired Area & Timeline curve)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
                        .testTag("timeline_canvas_graph")
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomY = height - 20f
                    val topY = 24f

                    val displayMinHour = if (selectedFilter == TimelinePeriodFilter.ALL) 6f else selectedFilter.startHour
                    val displayMaxHour = if (selectedFilter == TimelinePeriodFilter.ALL) 24f else selectedFilter.endHour
                    val hourRange = (displayMaxHour - displayMinHour).coerceAtLeast(1f)

                    fun hourToX(hour: Float): Float {
                        val clamped = hour.coerceIn(displayMinHour, displayMaxHour)
                        return ((clamped - displayMinHour) / hourRange) * width
                    }

                    // 1. Draw horizontal base timeline axis
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, bottomY),
                        end = Offset(width, bottomY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // 2. Draw vertical time grid lines and hour labels
                    val gridHours = if (selectedFilter == TimelinePeriodFilter.ALL) {
                        listOf(6f, 9f, 12f, 15f, 18f, 21f, 24f)
                    } else {
                        val step = (displayMaxHour - displayMinHour) / 4f
                        listOf(
                            displayMinHour,
                            displayMinHour + step,
                            displayMinHour + step * 2,
                            displayMinHour + step * 3,
                            displayMaxHour
                        )
                    }

                    gridHours.forEach { h ->
                        val x = hourToX(h)
                        drawLine(
                            color = outlineColor.copy(alpha = 0.35f),
                            start = Offset(x, topY),
                            end = Offset(x, bottomY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // 3. Build scheduled dose curve (Area fill + curve line)
                    val scheduledPoints = mutableListOf<Offset>()
                    val sortedMeds = medications.sortedBy { getSlotDecimalHour(it.timeSlot) }

                    // Default sample anchors if medications is empty
                    val effectivePoints = if (sortedMeds.isNotEmpty()) {
                        sortedMeds.map { med ->
                            val h = getSlotDecimalHour(med.timeSlot)
                            val x = hourToX(h)
                            val y = if (med.isTaken) (bottomY + topY) / 2f else topY + 10f
                            Offset(x, y)
                        }
                    } else {
                        listOf(
                            Offset(hourToX(8.5f), topY + 20f),
                            Offset(hourToX(13.5f), topY + 12f),
                            Offset(hourToX(19.0f), topY + 16f),
                            Offset(hourToX(21.5f), topY + 22f)
                        )
                    }

                    if (effectivePoints.isNotEmpty()) {
                        val curvePath = Path()
                        val areaPath = Path()

                        val firstX = effectivePoints.first().x.coerceAtLeast(0f)
                        curvePath.moveTo(0f, bottomY)
                        areaPath.moveTo(0f, bottomY)

                        var prevPoint = Offset(0f, bottomY)
                        effectivePoints.forEach { pt ->
                            val midX = (prevPoint.x + pt.x) / 2f
                            curvePath.cubicTo(midX, prevPoint.y, midX, pt.y, pt.x, pt.y)
                            areaPath.cubicTo(midX, prevPoint.y, midX, pt.y, pt.x, pt.y)
                            prevPoint = pt
                        }

                        // Close to right bottom
                        curvePath.cubicTo((prevPoint.x + width) / 2f, prevPoint.y, (prevPoint.x + width) / 2f, bottomY, width, bottomY)
                        areaPath.cubicTo((prevPoint.x + width) / 2f, prevPoint.y, (prevPoint.x + width) / 2f, bottomY, width, bottomY)
                        areaPath.lineTo(width, bottomY)
                        areaPath.close()

                        // Draw gradient area underneath curve
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = topY,
                                endY = bottomY
                            )
                        )

                        // Draw continuous timeline line
                        drawPath(
                            path = curvePath,
                            color = primaryColor,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // 4. Draw Current Time "NOW" Indicator
                    if (now in displayMinHour..displayMaxHour) {
                        val nowX = hourToX(now)
                        drawLine(
                            color = tertiaryColor,
                            start = Offset(nowX, 0f),
                            end = Offset(nowX, bottomY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                        drawCircle(
                            color = tertiaryColor,
                            radius = 5.dp.toPx(),
                            center = Offset(nowX, 6.dp.toPx())
                        )
                    }

                    // 5. Draw Medication Dose Nodes along the curve
                    effectivePoints.forEachIndexed { index, pt ->
                        val med = sortedMeds.getOrNull(index)
                        val isTaken = med?.isTaken == true
                        val nodeColor = if (isTaken) successColor else primaryColor

                        // Pulse halo for untaken upcoming dose
                        if (!isTaken) {
                            drawCircle(
                                color = primaryColor.copy(alpha = pulseAlpha * 0.4f),
                                radius = 12.dp.toPx(),
                                center = pt
                            )
                        }

                        // Outer ring
                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx(),
                            center = pt
                        )

                        // Center solid dot
                        drawCircle(
                            color = nodeColor,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Time Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("06:00 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("12:00 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("06:00 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("11:59 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            // Doses List for selected period
            val filteredMeds = if (selectedFilter == TimelinePeriodFilter.ALL) {
                medications
            } else {
                medications.filter { med ->
                    val h = getSlotDecimalHour(med.timeSlot)
                    h >= selectedFilter.startHour && h < selectedFilter.endHour
                }
            }

            if (filteredMeds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredMeds.forEach { med ->
                        TimelineMedicationItemCard(
                            medication = med,
                            onToggleTaken = { onToggleTaken(med.id) }
                        )
                    }
                }
            } else if (medications.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No medications scheduled for today yet. Add medicines to track timeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual medication dose card within the timeline visualization.
 */
@Composable
private fun TimelineMedicationItemCard(
    medication: TimelineDoseItem,
    onToggleTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTaken = medication.isTaken
    val slotIcon = when (medication.timeSlot.uppercase()) {
        "MORNING" -> Icons.Default.LightMode
        "AFTERNOON" -> Icons.Default.WbSunny
        "EVENING" -> Icons.Default.WbTwilight
        "NIGHT" -> Icons.Default.DarkMode
        else -> Icons.Default.Schedule
    }

    val timeLabel = when (medication.timeSlot.uppercase()) {
        "MORNING" -> "08:30 AM • Morning"
        "AFTERNOON" -> "01:30 PM • Afternoon"
        "EVENING" -> "07:00 PM • Evening"
        "NIGHT" -> "09:30 PM • Night"
        else -> medication.timeSlot
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isTaken) {
            Color(0xFFF0FDF4)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            if (isTaken) Color(0xFFBBF7D0) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTaken) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slotIcon,
                        contentDescription = null,
                        tint = if (isTaken) Color(0xFF166534) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = medication.dosage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Toggle Action Button
            Button(
                onClick = onToggleTaken,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTaken) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("med_timeline_toggle_${medication.id}")
            ) {
                if (isTaken) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Taken",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Taken", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                } else {
                    Text("Take", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Maps a timeSlot string or raw time into a decimal hour between 0.0 and 24.0
 */
private fun getSlotDecimalHour(timeSlot: String): Float {
    return when (timeSlot.uppercase().trim()) {
        "MORNING" -> 8.5f
        "AFTERNOON" -> 13.5f
        "EVENING" -> 19.0f
        "NIGHT" -> 21.5f
        else -> {
            // Attempt to parse custom formats like "08:30" or "8:00 AM"
            try {
                val cleaned = timeSlot.trim()
                val isPm = cleaned.contains("PM", ignoreCase = true)
                val isAm = cleaned.contains("AM", ignoreCase = true)
                val digits = cleaned.replace("AM", "", ignoreCase = true).replace("PM", "", ignoreCase = true).trim()
                val parts = digits.split(":")
                val h = parts[0].trim().toFloat()
                val m = if (parts.size > 1) parts[1].trim().toFloat() else 0f
                var finalH = h + (m / 60f)
                if (isPm && finalH < 12f) finalH += 12f
                if (isAm && finalH >= 12f) finalH -= 12f
                finalH
            } catch (_: Exception) {
                12.0f
            }
        }
    }
}

private fun formatTimeDec(hourDecimal: Float): String {
    val h = hourDecimal.toInt()
    val m = ((hourDecimal - h) * 60).toInt()
    val amPm = if (h >= 12) "PM" else "AM"
    val displayH = if (h % 12 == 0) 12 else h % 12
    return String.format("%02d:%02d %s", displayH, m, amPm)
}
