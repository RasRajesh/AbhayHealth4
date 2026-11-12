package com.example.abhayhealth.feature.symptoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.abhayhealth.R
import com.example.abhayhealth.core.designsystem.components.AbhayPrimaryButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSymptomDialog(
    onDismiss: () -> Unit,
    onSave: (symptomName: String, severity: String, severityScore: Int, notes: String, triggers: String, durationMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonSymptoms = listOf(
        "Headache", "Fever", "Fatigue", "Cough", "Nausea",
        "Dizziness", "Shortness of Breath", "Body Ache",
        "Chest Discomfort", "Stomach Pain", "Chills", "Anxiety"
    )

    val commonTriggers = listOf(
        "Stress", "Exercise", "After Meal", "Poor Sleep",
        "Weather", "Medication", "Dust", "None"
    )

    val durationOptions = listOf(
        15 to "15 min",
        30 to "30 min",
        60 to "1 hour",
        120 to "2 hours",
        240 to "4+ hours"
    )

    var selectedSymptom by remember { mutableStateOf(commonSymptoms.first()) }
    var customSymptom by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(SymptomLogEntity.SEVERITY_MILD) }
    var selectedTrigger by remember { mutableStateOf("None") }
    var selectedDuration by remember { mutableIntStateOf(30) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val formattedNow = remember {
        SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()).format(Date())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.symptoms_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Timestamp: $formattedNow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(14.dp))

                // Error Banner
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Section 1: Symptom Name
                Text(
                    text = "Select Symptom or Enter Custom",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonSymptoms.forEach { symptom ->
                        val isSelected = (selectedSymptom == symptom && customSymptom.isBlank())
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSymptom = symptom
                                customSymptom = ""
                                errorMessage = null
                            },
                            label = { Text(symptom, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customSymptom,
                    onValueChange = {
                        customSymptom = it
                        if (it.isNotBlank()) {
                            selectedSymptom = it.trim()
                        }
                        errorMessage = null
                    },
                    label = { Text("Or Type Other Symptom") },
                    placeholder = { Text("e.g. Migraine with aura, Rash") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("symptom_name_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Severity Level (Mild, Moderate, Severe, Critical)
                Text(
                    text = stringResource(id = R.string.symptoms_label_severity),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeverityChoiceCard(
                        title = "Mild",
                        score = 1,
                        isSelected = selectedSeverity == SymptomLogEntity.SEVERITY_MILD,
                        activeColor = Color(0xFF16A34A), // Green
                        activeContainer = Color(0xFFDCFCE7),
                        onClick = { selectedSeverity = SymptomLogEntity.SEVERITY_MILD },
                        modifier = Modifier.weight(1f)
                    )
                    SeverityChoiceCard(
                        title = "Moderate",
                        score = 2,
                        isSelected = selectedSeverity == SymptomLogEntity.SEVERITY_MODERATE,
                        activeColor = Color(0xFFD97706), // Amber
                        activeContainer = Color(0xFFFEF3C7),
                        onClick = { selectedSeverity = SymptomLogEntity.SEVERITY_MODERATE },
                        modifier = Modifier.weight(1f)
                    )
                    SeverityChoiceCard(
                        title = "Severe",
                        score = 3,
                        isSelected = selectedSeverity == SymptomLogEntity.SEVERITY_SEVERE,
                        activeColor = Color(0xFFEA580C), // Orange
                        activeContainer = Color(0xFFFFEDD5),
                        onClick = { selectedSeverity = SymptomLogEntity.SEVERITY_SEVERE },
                        modifier = Modifier.weight(1f)
                    )
                    SeverityChoiceCard(
                        title = "Critical",
                        score = 4,
                        isSelected = selectedSeverity == SymptomLogEntity.SEVERITY_CRITICAL,
                        activeColor = Color(0xFFDC2626), // Red
                        activeContainer = Color(0xFFFEE2E2),
                        onClick = { selectedSeverity = SymptomLogEntity.SEVERITY_CRITICAL },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 3: Triggers & Context
                Text(
                    text = "Possible Trigger (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonTriggers.forEach { trigger ->
                        FilterChip(
                            selected = selectedTrigger == trigger,
                            onClick = { selectedTrigger = trigger },
                            label = { Text(trigger, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 4: Duration
                Text(
                    text = "Estimated Duration",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durationOptions.forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedDuration == mins,
                            onClick = { selectedDuration = mins },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 5: Clinical Notes
                Text(
                    text = "Notes & Observations",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Describe sensation, location, or relief") },
                    placeholder = { Text("e.g. Throbbing in temples, relieved after drinking water") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("symptom_notes_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(id = R.string.profile_btn_cancel))
                    }

                    Button(
                        onClick = {
                            val finalName = if (customSymptom.isNotBlank()) customSymptom.trim() else selectedSymptom.trim()
                            if (finalName.isBlank()) {
                                errorMessage = "Please enter or select a symptom."
                                return@Button
                            }

                            val score = SymptomLogEntity.getScoreForSeverity(selectedSeverity)
                            onSave(
                                finalName,
                                selectedSeverity,
                                score,
                                notes.trim(),
                                selectedTrigger,
                                selectedDuration
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("symptom_save_button")
                    ) {
                        Text(stringResource(id = R.string.symptoms_btn_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityChoiceCard(
    title: String,
    score: Int,
    isSelected: Boolean,
    activeColor: Color,
    activeContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
