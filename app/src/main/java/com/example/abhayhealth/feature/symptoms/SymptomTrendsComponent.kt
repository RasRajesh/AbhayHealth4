package com.example.abhayhealth.feature.symptoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.abhayhealth.R
import com.example.abhayhealth.core.designsystem.components.AbhayPrimaryButton
import com.example.abhayhealth.core.designsystem.components.EmptyStateContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomTrendsComponent(
    symptoms: List<SymptomLogEntity>,
    onAddSymptomClick: () -> Unit,
    onDeleteSymptomClick: (SymptomLogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: 7 Days, 2: 30 Days
    var symptomToDelete by remember { mutableStateOf<SymptomLogEntity?>(null) }

    val now = System.currentTimeMillis()
    val filteredSymptoms = remember(symptoms, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> {
                val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
                symptoms.filter { it.timestamp >= sevenDaysAgo }
            }
            2 -> {
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                symptoms.filter { it.timestamp >= thirtyDaysAgo }
            }
            else -> symptoms
        }
    }

    val totalCount = filteredSymptoms.size
    val mildCount = filteredSymptoms.count { it.severity == SymptomLogEntity.SEVERITY_MILD }
    val moderateCount = filteredSymptoms.count { it.severity == SymptomLogEntity.SEVERITY_MODERATE }
    val severeCount = filteredSymptoms.count { it.severity == SymptomLogEntity.SEVERITY_SEVERE }
    val criticalCount = filteredSymptoms.count { it.severity == SymptomLogEntity.SEVERITY_CRITICAL }

    val mostCommonSymptom = remember(filteredSymptoms) {
        if (filteredSymptoms.isEmpty()) "None"
        else filteredSymptoms.groupBy { it.symptomName }
            .maxByOrNull { it.value.size }
            ?.let { "${it.key} (${it.value.size}x)" } ?: "None"
    }

    val avgSeverityScore = remember(filteredSymptoms) {
        if (filteredSymptoms.isEmpty()) 0.0
        else filteredSymptoms.map { it.severityScore }.average()
    }

    // Delete Confirmation Dialog
    if (symptomToDelete != null) {
        AlertDialog(
            onDismissRequest = { symptomToDelete = null },
            title = { Text("Delete Symptom Record?") },
            text = {
                Text("Are you sure you want to remove the log for '${symptomToDelete?.symptomName}'? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        symptomToDelete?.let(onDeleteSymptomClick)
                        symptomToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { symptomToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.symptoms_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.symptoms_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAddSymptomClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("log_symptom_main_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Log",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Range Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All Time", "Past 7 Days", "Past 30 Days").forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (symptoms.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyStateContent(
                        title = stringResource(id = R.string.symptoms_empty_title),
                        description = stringResource(id = R.string.symptoms_empty_desc),
                        icon = Icons.Default.Info
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AbhayPrimaryButton(
                        text = stringResource(id = R.string.symptoms_btn_log),
                        icon = Icons.Default.Add,
                        onClick = onAddSymptomClick,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }
            }
        } else {
            // Analytics Overview Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsStatCard(
                    title = "Total Logs",
                    value = totalCount.toString(),
                    subtitle = "Entries in period",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Frequent",
                    value = mostCommonSymptom,
                    subtitle = "Highest recurrence",
                    modifier = Modifier.weight(1.3f)
                )
                AnalyticsStatCard(
                    title = "Avg Severity",
                    value = String.format(Locale.US, "%.1f / 4", avgSeverityScore),
                    subtitle = getSeverityDescriptor(avgSeverityScore),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Severity Distribution Bar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Severity Distribution",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-segmented bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        if (totalCount > 0) {
                            if (mildCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(mildCount.toFloat())
                                        .height(8.dp)
                                        .background(Color(0xFF16A34A))
                                )
                            }
                            if (moderateCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(moderateCount.toFloat())
                                        .height(8.dp)
                                        .background(Color(0xFFD97706))
                                )
                            }
                            if (severeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(severeCount.toFloat())
                                        .height(8.dp)
                                        .background(Color(0xFFEA580C))
                                )
                            }
                            if (criticalCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(criticalCount.toFloat())
                                        .height(8.dp)
                                        .background(Color(0xFFDC2626))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SeverityLegendItem(label = "Mild", count = mildCount, color = Color(0xFF16A34A))
                        SeverityLegendItem(label = "Moderate", count = moderateCount, color = Color(0xFFD97706))
                        SeverityLegendItem(label = "Severe", count = severeCount, color = Color(0xFFEA580C))
                        SeverityLegendItem(label = "Critical", count = criticalCount, color = Color(0xFFDC2626))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chronological Symptom Timeline Cards
            Text(
                text = "Recent Symptom Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredSymptoms.forEach { item ->
                    SymptomRecordCard(
                        symptom = item,
                        onDeleteClick = { symptomToDelete = item }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SeverityLegendItem(
    label: String,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SymptomRecordCard(
    symptom: SymptomLogEntity,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (severityColor, severityBg) = when (symptom.severity.uppercase()) {
        SymptomLogEntity.SEVERITY_MILD -> Color(0xFF16A34A) to Color(0xFFDCFCE7)
        SymptomLogEntity.SEVERITY_MODERATE -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        SymptomLogEntity.SEVERITY_SEVERE -> Color(0xFFEA580C) to Color(0xFFFFEDD5)
        SymptomLogEntity.SEVERITY_CRITICAL -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(symptom.timestamp) { dateFormat.format(Date(symptom.timestamp)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Severity Color Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(95.dp)
                    .background(severityColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = symptom.symptomName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Severity Pill
                        Surface(
                            color = severityBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = symptom.severity,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = severityColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete symptom log",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Triggers and Duration Badges
                if (symptom.triggers.isNotBlank() && symptom.triggers != "None" || symptom.durationMinutes > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (symptom.triggers.isNotBlank() && symptom.triggers != "None") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Trigger: ${symptom.triggers}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (symptom.durationMinutes > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "⏱ ${symptom.durationMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (symptom.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "“${symptom.notes}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun getSeverityDescriptor(score: Double): String {
    return when {
        score < 1.5 -> "Mostly Mild"
        score < 2.5 -> "Moderate Range"
        score < 3.5 -> "Elevated Severity"
        else -> "High Severity"
    }
}
