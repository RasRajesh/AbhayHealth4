package com.example.abhayhealth.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.domain.model.BiomarkerStatus
import com.example.abhayhealth.domain.model.OrganSystem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abhayhealth.R

/**
 * High-tech Medical Infographic Data Visualization.
 * Features a central translucent 3D human body silhouette with isometric perspective,
 * surrounded by modern digital callout boxes for Blood Pressure, Blood Sugar, Heart Rate, and SpO2,
 * plus interactive telemetry pills for extracted clinical biomarkers.
 */
@Composable
fun MedicalInfographicCard(
    bloodPressure: String = "120/80 mmHg",
    bloodSugar: String = "95 mg/dL",
    heartRate: String = "72 BPM",
    spO2: String = "98%",
    extractedBiomarkers: List<ParsedMedicalResult> = emptyList(),
    onMetricClick: (String, ParsedMedicalResult?) -> Unit = { _, _ -> },
    onLogVitalClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val darkSlateBg = Color(0xFF0F172A)
    val neonBlue = Color(0xFF38BDF8)
    val neonCyan = Color(0xFF06B6D4)
    val softWhite = Color(0xFFF8FAFC)
    val cardBorder = Color(0xFF1E293B)

    // Pulsing animation for high-tech HUD effect
    val infiniteTransition = rememberInfiniteTransition(label = "hudPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseProgress"
    )

    var selectedOrganSystem by remember { mutableStateOf(OrganSystem.ALL) }
    val outOfRangeCount = remember(extractedBiomarkers) { extractedBiomarkers.count { it.isOutOfRange } }
    val displayBiomarkers = remember(extractedBiomarkers, selectedOrganSystem) {
        if (selectedOrganSystem == OrganSystem.ALL) {
            extractedBiomarkers
        } else {
            extractedBiomarkers.filter { it.organSystem == selectedOrganSystem }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medical_infographic_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = darkSlateBg),
        border = BorderStroke(1.dp, neonBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Clean unconstrained title and + Log Vital button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = stringResource(id = R.string.home_infographic_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = softWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(id = R.string.home_infographic_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onLogVitalClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = neonBlue.copy(alpha = 0.22f),
                        contentColor = neonBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Log Vital", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Out-of-Range Clinical Warning Banner
            if (outOfRangeCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF451A03), // Deep Amber/Red
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Text(
                            text = "⚠ $outOfRangeCount LAB MARKERS REQUIRE PHYSICIAN REVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Central Translucent 3D Silhouette + Connecting Telemetry Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF090D16))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                TranslucentBodyCanvas(
                    pulse = pulseProgress,
                    neonBlue = neonBlue,
                    neonCyan = neonCyan,
                    selectedOrgan = selectedOrganSystem
                )

                // High-tech medical telemetry HUD overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(neonCyan)
                        )
                        Text(
                            text = "ANATOMICAL TELEMETRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = neonCyan
                        )
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = Color(0xFF1E293B).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = selectedOrganSystem.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = neonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "REAL-TIME PHYSIOLOGICAL MAPPING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Surrounding Digital Callout Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCalloutBox(
                    title = "BLOOD PRESSURE",
                    value = bloodPressure,
                    status = "120/80 Optimal",
                    accentColor = Color(0xFF38BDF8),
                    icon = Icons.Default.Speed,
                    onClick = { onMetricClick("Blood Pressure", null) },
                    modifier = Modifier.weight(1f)
                )
                MetricCalloutBox(
                    title = "BLOOD SUGAR",
                    value = bloodSugar,
                    status = "95 mg/dL Fasting",
                    accentColor = Color(0xFF34D399),
                    icon = Icons.Default.Science,
                    onClick = { onMetricClick("Blood Sugar", null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCalloutBox(
                    title = "HEART RATE",
                    value = heartRate,
                    status = "72 BPM Resting",
                    accentColor = Color(0xFFF43F5E),
                    icon = Icons.Default.MonitorHeart,
                    onClick = { onMetricClick("Heart Rate", null) },
                    modifier = Modifier.weight(1f)
                )
                MetricCalloutBox(
                    title = "SPO2 (OXYGEN)",
                    value = spO2,
                    status = "98% Arterial",
                    accentColor = Color(0xFF60A5FA),
                    icon = Icons.Default.Air,
                    onClick = { onMetricClick("SpO2", null) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (extractedBiomarkers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = cardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Organ System Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OrganSystem.values().forEach { organ ->
                        val count = if (organ == OrganSystem.ALL) {
                            extractedBiomarkers.size
                        } else {
                            extractedBiomarkers.count { it.organSystem == organ }
                        }
                        val isSelected = selectedOrganSystem == organ

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) neonCyan.copy(alpha = 0.22f) else Color(0xFF131D31),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) neonCyan else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.clickable { selectedOrganSystem = organ }
                        ) {
                            Text(
                                text = "${organ.displayName} ($count)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) neonCyan else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXTRACTED CLINICAL BIOMARKERS [${selectedOrganSystem.code}]",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = neonCyan
                    )
                    Text(
                        text = "${displayBiomarkers.size} METRICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayBiomarkers.take(25).forEach { bio ->
                        val status = bio.status
                        val statusColor = when (status) {
                            BiomarkerStatus.CRITICAL_HIGH, BiomarkerStatus.CRITICAL_LOW -> Color(0xFFEF4444)
                            BiomarkerStatus.HIGH, BiomarkerStatus.LOW, BiomarkerStatus.QUALITATIVE_ABNORMAL -> Color(0xFFF59E0B)
                            BiomarkerStatus.NORMAL -> Color(0xFF10B981)
                            else -> Color(0xFF94A3B8)
                        }

                        val statusBadge = when (status) {
                            BiomarkerStatus.CRITICAL_HIGH -> "CRIT ▲"
                            BiomarkerStatus.CRITICAL_LOW -> "CRIT ▼"
                            BiomarkerStatus.HIGH -> "HIGH ▲"
                            BiomarkerStatus.LOW -> "LOW ▼"
                            BiomarkerStatus.QUALITATIVE_ABNORMAL -> "ABN ⚠"
                            BiomarkerStatus.NORMAL -> "NORM ✓"
                            else -> ""
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF131D31),
                            border = BorderStroke(
                                1.dp,
                                if (bio.isOutOfRange) statusColor.copy(alpha = 0.7f) else neonBlue.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.clickable { onMetricClick(bio.testName, bio) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = bio.testName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFF94A3B8)
                                    )
                                    if (statusBadge.isNotEmpty()) {
                                        Text(
                                            text = statusBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = statusColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${bio.value} ${bio.unit}".trim(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (bio.isOutOfRange) statusColor else neonBlue
                                )
                                if (bio.referenceRange.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "Ref: ${bio.referenceRange}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 7.5.sp
                                        ),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Renders the central translucent 3D human body silhouette with isometric perspective,
 * isometric floor grid, scanning core, and glowing biometric telemetry locator nodes.
 */
@Composable
private fun TranslucentBodyCanvas(
    pulse: Float,
    neonBlue: Color,
    neonCyan: Color,
    selectedOrgan: OrganSystem = OrganSystem.ALL
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Ambient Medical Diagnostic Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    neonCyan.copy(alpha = 0.16f),
                    neonBlue.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx, cy - 10f),
                radius = h * 0.55f
            ),
            radius = h * 0.55f,
            center = Offset(cx, cy - 10f)
        )

        // 2. High-Tech Isometric Base Pedestal
        val floorY = h * 0.84f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(neonCyan.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(cx, floorY),
                radius = 85f
            ),
            topLeft = Offset(cx - 75f, floorY - 14f),
            size = Size(150f, 28f)
        )
        drawOval(
            color = neonCyan.copy(alpha = 0.45f),
            topLeft = Offset(cx - 65f, floorY - 12f),
            size = Size(130f, 24f),
            style = Stroke(width = 1.5f)
        )
        drawOval(
            color = neonBlue.copy(alpha = 0.2f),
            topLeft = Offset(cx - 90f, floorY - 16f),
            size = Size(180f, 32f),
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
        )

        // 3. Realistic Anatomical Human Body Silhouette (Smooth Curves)
        val headY = h * 0.11f
        val headRadius = 14f

        // Head & Cranium
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), neonCyan.copy(alpha = 0.35f), neonBlue.copy(alpha = 0.15f)),
                center = Offset(cx, headY),
                radius = headRadius
            ),
            radius = headRadius,
            center = Offset(cx, headY)
        )
        drawCircle(
            color = neonCyan.copy(alpha = 0.85f),
            radius = headRadius,
            center = Offset(cx, headY),
            style = Stroke(width = 1.5f)
        )

        // Smooth Organic Body Path (Trapezius, Shoulders, Deltoids, Chest, Waist, Hips, Legs)
        val bodyPath = Path().apply {
            // Start at neck
            moveTo(cx - 5f, headY + headRadius)
            // Trapezius & Left Shoulder Curve
            quadraticBezierTo(cx - 16f, headY + headRadius + 4f, cx - 26f, headY + headRadius + 12f)
            // Left Deltoid & Bicep Curve
            quadraticBezierTo(cx - 36f, headY + headRadius + 28f, cx - 38f, headY + headRadius + 46f)
            // Left Forearm to Wrist
            quadraticBezierTo(cx - 42f, headY + headRadius + 64f, cx - 44f, headY + headRadius + 76f)
            // Left Hand
            lineTo(cx - 36f, headY + headRadius + 76f)
            // Inner Forearm & Armpit
            quadraticBezierTo(cx - 34f, headY + headRadius + 60f, cx - 26f, headY + headRadius + 42f)
            // Left Chest & Waist Curve
            quadraticBezierTo(cx - 20f, headY + headRadius + 58f, cx - 22f, headY + headRadius + 70f)
            // Left Hip & Thigh
            quadraticBezierTo(cx - 24f, headY + headRadius + 84f, cx - 18f, headY + headRadius + 110f)
            // Left Knee & Calf
            quadraticBezierTo(cx - 16f, floorY - 24f, cx - 17f, floorY - 8f)
            // Left Ankle & Foot
            lineTo(cx - 7f, floorY - 8f)
            // Inner Left Leg to Crotch
            quadraticBezierTo(cx - 9f, headY + headRadius + 110f, cx - 2f, headY + headRadius + 90f)
            // Crotch
            lineTo(cx + 2f, headY + headRadius + 90f)
            // Inner Right Leg from Crotch
            quadraticBezierTo(cx + 9f, headY + headRadius + 110f, cx + 7f, floorY - 8f)
            // Right Ankle & Foot
            lineTo(cx + 17f, floorY - 8f)
            // Right Calf & Knee
            quadraticBezierTo(cx + 16f, floorY - 24f, cx + 18f, headY + headRadius + 110f)
            // Right Thigh & Hip
            quadraticBezierTo(cx + 24f, headY + headRadius + 84f, cx + 22f, headY + headRadius + 70f)
            // Right Waist & Chest
            quadraticBezierTo(cx + 20f, headY + headRadius + 58f, cx + 26f, headY + headRadius + 42f)
            // Inner Right Arm
            quadraticBezierTo(cx + 34f, headY + headRadius + 60f, cx + 36f, headY + headRadius + 76f)
            // Right Hand
            lineTo(cx + 44f, headY + headRadius + 76f)
            // Right Forearm & Bicep
            quadraticBezierTo(cx + 42f, headY + headRadius + 64f, cx + 38f, headY + headRadius + 46f)
            // Right Deltoid
            quadraticBezierTo(cx + 36f, headY + headRadius + 28f, cx + 26f, headY + headRadius + 12f)
            // Right Trapezius to Neck
            quadraticBezierTo(cx + 16f, headY + headRadius + 4f, cx + 5f, headY + headRadius)
            close()
        }

        // Translucent Anatomical Gradient Fill
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    neonCyan.copy(alpha = 0.38f),
                    neonBlue.copy(alpha = 0.24f),
                    Color(0xFF0284C7).copy(alpha = 0.20f),
                    Color(0xFF1E3A8A).copy(alpha = 0.15f)
                ),
                startY = headY,
                endY = floorY
            )
        )

        // Holographic Outer Silhouette Stroke
        drawPath(
            path = bodyPath,
            color = neonCyan.copy(alpha = 0.7f),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )

        // 4. Glowing Internal Anatomical Organs
        val chestCenterY = headY + headRadius + 32f

        // Respiratory: Bilateral Lung Fields
        val lungAlpha = if (selectedOrgan == OrganSystem.HEMIC || selectedOrgan == OrganSystem.ALL) 0.35f else 0.15f
        drawOval(
            color = neonCyan.copy(alpha = lungAlpha),
            topLeft = Offset(cx - 19f, chestCenterY - 14f),
            size = Size(13f, 24f)
        )
        drawOval(
            color = neonCyan.copy(alpha = lungAlpha),
            topLeft = Offset(cx + 6f, chestCenterY - 14f),
            size = Size(13f, 24f)
        )

        // Cardiovascular: Heart Core with Rhythmic Arterial Heartbeat Pulse
        val heartPos = Offset(cx - 8f, chestCenterY - 2f)
        val heartBeatScale = 1f + 0.35f * kotlin.math.sin(pulse * Math.PI.toFloat() * 2f).coerceAtLeast(0f)
        val heartAlpha = if (selectedOrgan == OrganSystem.CARDIO || selectedOrgan == OrganSystem.ALL) 0.9f else 0.5f

        // Expanding cardiac pulse wave
        drawCircle(
            color = Color(0xFFF43F5E).copy(alpha = 0.25f * (2f - heartBeatScale)),
            radius = 12f * heartBeatScale,
            center = heartPos
        )
        // Solid pulsing heart core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFF43F5E), Color(0xFFE11D48).copy(alpha = 0.7f)),
                center = heartPos,
                radius = 7f * heartBeatScale
            ),
            radius = 6f * heartBeatScale,
            center = heartPos
        )

        // Endocrine / Pancreas Node (Upper Abdomen)
        val pancreasPos = Offset(cx + 2f, chestCenterY + 22f)
        drawCircle(
            color = Color(0xFF34D399).copy(alpha = if (selectedOrgan == OrganSystem.METABOLIC || selectedOrgan == OrganSystem.ALL) 0.8f else 0.4f),
            radius = 4.5f,
            center = pancreasPos
        )

        // Renal: Kidneys (Bilateral Flank Nodes)
        val kidneyAlpha = if (selectedOrgan == OrganSystem.RENAL || selectedOrgan == OrganSystem.ALL) 0.75f else 0.3f
        drawCircle(color = Color(0xFFA855F7).copy(alpha = kidneyAlpha), radius = 3.5f, center = Offset(cx - 12f, chestCenterY + 28f))
        drawCircle(color = Color(0xFFA855F7).copy(alpha = kidneyAlpha), radius = 3.5f, center = Offset(cx + 12f, chestCenterY + 28f))

        // Spinal Telemetry Line
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.7f), neonCyan.copy(alpha = 0.5f), Color.Transparent),
                startY = headY + headRadius + 4f,
                endY = floorY - 30f
            ),
            start = Offset(cx, headY + headRadius + 4f),
            end = Offset(cx, floorY - 30f),
            strokeWidth = 1.2f
        )

        // 5. Smooth Horizontal Laser Scanning Sweep
        val scanY = headY + (floorY - headY) * pulse
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    neonCyan.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.9f),
                    neonCyan.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                startX = cx - 60f,
                endX = cx + 60f
            ),
            start = Offset(cx - 60f, scanY),
            end = Offset(cx + 60f, scanY),
            strokeWidth = 1.8f
        )

        // 6. Connected Telemetry Leader Lines to Peripheral Metrics
        val bpPos = Offset(cx + 40f, headY + headRadius + 70f)
        drawBeaconNode(bpPos, Color(0xFF38BDF8), pulse)
        drawLeaderLine(
            start = bpPos,
            mid = Offset(bpPos.x + 22f, bpPos.y - 10f),
            end = Offset(w - 18f, bpPos.y - 10f),
            color = Color(0xFF38BDF8).copy(alpha = 0.6f)
        )

        drawBeaconNode(heartPos, Color(0xFFF43F5E), pulse)
        drawLeaderLine(
            start = heartPos,
            mid = Offset(heartPos.x - 26f, heartPos.y - 12f),
            end = Offset(18f, heartPos.y - 12f),
            color = Color(0xFFF43F5E).copy(alpha = 0.6f)
        )

        drawBeaconNode(pancreasPos, Color(0xFF34D399), pulse)
        drawLeaderLine(
            start = pancreasPos,
            mid = Offset(pancreasPos.x + 32f, pancreasPos.y + 16f),
            end = Offset(w - 18f, pancreasPos.y + 16f),
            color = Color(0xFF34D399).copy(alpha = 0.6f)
        )

        val spo2Pos = Offset(cx - 14f, chestCenterY - 10f)
        drawBeaconNode(spo2Pos, Color(0xFF60A5FA), pulse)
        drawLeaderLine(
            start = spo2Pos,
            mid = Offset(spo2Pos.x - 30f, spo2Pos.y + 18f),
            end = Offset(18f, spo2Pos.y + 18f),
            color = Color(0xFF60A5FA).copy(alpha = 0.6f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeaconNode(
    center: Offset,
    accentColor: Color,
    pulse: Float
) {
    // Outer expanding glow ring
    drawCircle(
        color = accentColor.copy(alpha = (1f - pulse) * 0.45f),
        radius = 8f + (pulse * 8f),
        center = center,
        style = Stroke(width = 1.5f)
    )
    // Static base ring
    drawCircle(
        color = Color.White,
        radius = 5f,
        center = center
    )
    // Bright core
    drawCircle(
        color = accentColor,
        radius = 3.5f,
        center = center
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLeaderLine(
    start: Offset,
    mid: Offset,
    end: Offset,
    color: Color
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        lineTo(mid.x, mid.y)
        lineTo(end.x, end.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 1.4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
    )
    drawCircle(
        color = color,
        radius = 2.5f,
        center = end
    )
}

/**
 * Modern High-Tech Callout Box displaying an individual health metric
 * with neon accent, clean typography, icon, and status badge.
 */
@Composable
private fun MetricCalloutBox(
    title: String,
    value: String,
    status: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("metric_callout_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF131D31),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accentColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
