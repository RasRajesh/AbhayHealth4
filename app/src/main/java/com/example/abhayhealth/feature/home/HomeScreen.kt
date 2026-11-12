package com.example.abhayhealth.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abhayhealth.R
import com.example.abhayhealth.core.designsystem.components.AbhayHealthBrandLogo
import com.example.abhayhealth.core.designsystem.components.AbhayPrimaryButton
import com.example.abhayhealth.core.designsystem.components.EmptyStateContent
import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.domain.model.BiomarkerStatus
import com.example.abhayhealth.feature.medicines.PrescriptionMedicationEntity
import com.example.abhayhealth.feature.medicines.MedicationAdvisoryHelper
import com.example.abhayhealth.feature.medicines.MedicationDailyTimelineGraph
import com.example.abhayhealth.feature.records.ExtractedMedicalInfo
import com.example.abhayhealth.feature.vitals.AddVitalReadingDialog
import com.example.abhayhealth.feature.vitals.HealthTrendsChartCard
import com.example.abhayhealth.feature.vitals.VitalType

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddRecordClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val patientProfile by viewModel.patientProfile.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()
    val aiSummaryState by viewModel.aiSummaryState.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val doctorBriefText by viewModel.doctorBriefText.collectAsState()
    val medicationSafetyReport by viewModel.medicationSafetyReport.collectAsState()
    val vitalReadings by viewModel.vitalReadings.collectAsState()
    val selectedVitalMetric by viewModel.selectedVitalMetric.collectAsState()
    val showAddVitalDialog by viewModel.showAddVitalDialog.collectAsState()
    val extractedInfos by viewModel.extractedInfos.collectAsState()
    val extractedBiomarkers by viewModel.extractedBiomarkers.collectAsState()
    val todayMedications by viewModel.todayMedications.collectAsState()
    var selectedBiomarkerModal by remember { mutableStateOf<SelectedBiomarkerModal?>(null) }

    val greetingText = if (!patientName.isNullOrBlank()) {
        stringResource(id = R.string.home_greeting_user, patientName!!)
    } else {
        stringResource(id = R.string.home_greeting_default)
    }

    val latestBp = remember(vitalReadings, extractedBiomarkers) {
        val bp = vitalReadings.firstOrNull { it.type == "BLOOD_PRESSURE" && it.systolic != null && it.diastolic != null }
        if (bp != null) {
            "${bp.systolic?.toInt()}/${bp.diastolic?.toInt()} mmHg"
        } else {
            val bpBio = extractedBiomarkers.firstOrNull {
                it.testName.contains("Blood Pressure", ignoreCase = true) || it.testName.equals("BP", ignoreCase = true)
            }
            if (bpBio != null) "${bpBio.value} ${bpBio.unit}".trim() else "120/80 mmHg"
        }
    }
    val latestSugar = remember(extractedBiomarkers) {
        val sugar = extractedBiomarkers.firstOrNull {
            it.testName.contains("Glucose", ignoreCase = true) || it.testName.contains("Sugar", ignoreCase = true) || it.testName.contains("FBS", ignoreCase = true)
        }
        if (sugar != null) "${sugar.value} ${sugar.unit}".trim() else "95 mg/dL"
    }
    val latestHr = remember(extractedBiomarkers) {
        val hr = extractedBiomarkers.firstOrNull {
            it.testName.contains("Heart", ignoreCase = true) || it.testName.contains("Pulse", ignoreCase = true)
        }
        if (hr != null) "${hr.value} BPM".trim() else "72 BPM"
    }
    val latestSpo2 = remember(extractedBiomarkers) {
        val spo2 = extractedBiomarkers.firstOrNull {
            it.testName.contains("SpO2", ignoreCase = true) || it.testName.contains("Oxygen", ignoreCase = true)
        }
        if (spo2 != null) {
            val raw = spo2.value.trim()
            if (raw.endsWith("%")) raw else "$raw%"
        } else "98%"
    }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Greeting & Brand / Profile Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.home_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                val photoPath = patientProfile?.photoUri
                val profileBitmap = remember(photoPath) {
                    if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
                        BitmapFactory.decodeFile(photoPath)
                    } else null
                }
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap.asImageBitmap(),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                } else {
                    AbhayHealthBrandLogo(
                        size = 46.dp,
                        elevation = 2.dp
                    )
                }
            }
        }

        // 2. Biometric Health Telemeter (Central Medical Infographic Silhouette & Clinical Telemetry)
        item {
            MedicalInfographicCard(
                bloodPressure = latestBp,
                bloodSugar = latestSugar,
                heartRate = latestHr,
                spO2 = latestSpo2,
                extractedBiomarkers = extractedBiomarkers,
                onLogVitalClick = {
                    viewModel.showAddVitalDialog(true)
                },
                onMetricClick = { name, bio ->
                    val displayVal = when (name.lowercase()) {
                        "blood pressure" -> latestBp
                        "blood sugar" -> latestSugar
                        "heart rate" -> latestHr
                        "spo2" -> latestSpo2
                        else -> bio?.let { "${it.value} ${it.unit}".trim() }
                    }
                    selectedBiomarkerModal = SelectedBiomarkerModal(name, bio, displayVal)
                }
            )
        }



        // 4. Today's Medications Section with Daily Dose Timeline Graph
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.home_section_meds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (todayMedications.isNotEmpty()) {
                        val takenCount = todayMedications.count { it.isTaken }
                        Text(
                            text = "$takenCount/${todayMedications.size} taken",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Daily Medication Dose Timeline Visualization
                MedicationDailyTimelineGraph(
                    medications = todayMedications,
                    onToggleTaken = { id -> viewModel.toggleMedicationTaken(id) }
                )

                if (todayMedications.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        todayMedications.forEach { medication ->
                            HomeMedicationItemRow(
                                medication = medication,
                                onToggleTaken = { viewModel.toggleMedicationTaken(medication.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive AI Dialogs
    when (activeDialog) {
        AiDialogType.AI_ASSISTANT_MODAL -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "AI Health Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Health Status: ${aiSummaryState.statusTier.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Score: ${aiSummaryState.healthScore}/100",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiSummaryState.summarySubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Text(
                            text = "Specialized AI Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 1. Doctor Brief Action
                        FilledTonalButton(
                            onClick = { viewModel.showDialog(AiDialogType.DOCTOR_BRIEF) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Doctor Consult Brief", fontWeight = FontWeight.SemiBold)
                        }

                        // 2. Medication Safety Action
                        FilledTonalButton(
                            onClick = { viewModel.showDialog(AiDialogType.MEDICATION_SAFETY) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Medication Safety Audit", fontWeight = FontWeight.SemiBold)
                        }

                        // 3. Lab Explainer Action
                        FilledTonalButton(
                            onClick = { viewModel.showDialog(AiDialogType.LAB_EXPLAINER) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explain Medical Lab Reports", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.refreshAiSummary()
                            viewModel.dismissDialog()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh AI Analysis")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Close")
                    }
                }
            )
        }

        AiDialogType.DOCTOR_BRIEF -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.home_ai_chip_brief),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = doctorBriefText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Doctor Consult Brief", doctorBriefText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.home_ai_dialog_copied, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.home_ai_dialog_copy))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(id = R.string.home_ai_dialog_close))
                    }
                }
            )
        }

        AiDialogType.MEDICATION_SAFETY -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.home_ai_chip_interaction),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = medicationSafetyReport,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(id = R.string.home_ai_dialog_close))
                    }
                }
            )
        }

        AiDialogType.LAB_EXPLAINER -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.home_ai_chip_explainer),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LabExplainerItem(
                            title = "Fasting Blood Sugar (FBS)",
                            normalRange = "70 – 99 mg/dL",
                            meaning = "Measures circulating glucose after an 8-hour fast. Crucial indicator for prediabetes and glycemic stability."
                        )
                        LabExplainerItem(
                            title = "HbA1c (Glycated Hemoglobin)",
                            normalRange = "< 5.7% (Normal), 5.7–6.4% (Pre-diabetic)",
                            meaning = "Reflects average blood sugar control over the preceding 2 to 3 months."
                        )
                        LabExplainerItem(
                            title = "Blood Pressure (BP)",
                            normalRange = "< 120/80 mmHg",
                            meaning = "Systolic over diastolic arterial pressure. Key for cardiovascular and renal risk surveillance."
                        )
                        LabExplainerItem(
                            title = "Hemoglobin (Hb)",
                            normalRange = "12.0 – 17.5 g/dL",
                            meaning = "Oxygen-carrying protein in red blood cells. Low values signify anemia; high values suggest dehydration or polycythemia."
                        )
                        LabExplainerItem(
                            title = "Serum Creatinine",
                            normalRange = "0.7 – 1.3 mg/dL",
                            meaning = "Filtration waste byproduct evaluated to assess kidney health and glomerular filtration efficacy."
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(id = R.string.home_ai_dialog_close))
                    }
                }
            )
        }

        AiDialogType.NONE -> { /* No dialog shown */ }
    }

    if (showAddVitalDialog) {
        AddVitalReadingDialog(
            initialMetric = selectedVitalMetric,
            onDismissRequest = { viewModel.showAddVitalDialog(false) },
            onSaveReading = { type, systolic, diastolic, weightKg, notes ->
                viewModel.addVitalReading(type, systolic, diastolic, weightKg, notes)
            }
        )
    }

    selectedBiomarkerModal?.let { modal ->
        BiomarkerDetailDialog(
            modal = modal,
            onDismiss = { selectedBiomarkerModal = null },
            onLogVital = {
                val metricType = if (modal.name.contains("weight", ignoreCase = true)) {
                    VitalType.WEIGHT
                } else {
                    VitalType.BLOOD_PRESSURE
                }
                viewModel.selectVitalMetric(metricType)
                selectedBiomarkerModal = null
                viewModel.showAddVitalDialog(true)
            }
        )
    }
}

data class SelectedBiomarkerModal(
    val name: String,
    val biomarker: ParsedMedicalResult? = null,
    val displayValue: String? = null
)

@Composable
fun BiomarkerDetailDialog(
    modal: SelectedBiomarkerModal,
    onDismiss: () -> Unit,
    onLogVital: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bio = modal.biomarker
    val valueStr = modal.displayValue ?: bio?.let { "${it.value} ${it.unit}".trim() } ?: "--"
    
    val status = bio?.status ?: when (modal.name.lowercase()) {
        "blood pressure" -> BiomarkerStatus.NORMAL
        "blood sugar" -> BiomarkerStatus.NORMAL
        "heart rate" -> BiomarkerStatus.NORMAL
        "spo2" -> BiomarkerStatus.NORMAL
        else -> BiomarkerStatus.UNKNOWN
    }

    val statusColor = when (status) {
        BiomarkerStatus.CRITICAL_HIGH, BiomarkerStatus.CRITICAL_LOW -> MaterialTheme.colorScheme.error
        BiomarkerStatus.HIGH, BiomarkerStatus.LOW, BiomarkerStatus.QUALITATIVE_ABNORMAL -> Color(0xFFF59E0B)
        BiomarkerStatus.NORMAL -> Color(0xFF10B981)
        BiomarkerStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    val statusLabel = when (status) {
        BiomarkerStatus.CRITICAL_HIGH -> "Critical High ▲"
        BiomarkerStatus.CRITICAL_LOW -> "Critical Low ▼"
        BiomarkerStatus.HIGH -> "High / Elevated ▲"
        BiomarkerStatus.LOW -> "Low ▼"
        BiomarkerStatus.QUALITATIVE_ABNORMAL -> "Abnormal ⚠"
        BiomarkerStatus.NORMAL -> "Normal / Optimal ✓"
        BiomarkerStatus.UNKNOWN -> "Reported"
    }

    val referenceRange = bio?.referenceRange?.ifBlank { null } ?: when (modal.name.lowercase()) {
        "blood pressure", "bp" -> "< 120/80 mmHg (Normal adult)"
        "blood sugar", "glucose", "fbs" -> "70 – 99 mg/dL (Fasting), < 140 mg/dL (Post-Meal)"
        "heart rate", "pulse" -> "60 – 100 BPM (Resting)"
        "spo2", "oxygen" -> "95% – 100% (Arterial saturation)"
        else -> "Standard physiological clinical reference range"
    }

    val organSystemName = bio?.organSystem?.displayName ?: when (modal.name.lowercase()) {
        "blood pressure", "bp", "heart rate", "pulse" -> "Cardiovascular System"
        "blood sugar", "glucose", "fbs" -> "Endocrine & Metabolic"
        "spo2", "oxygen" -> "Respiratory System"
        else -> "General Health Telemetry"
    }

    val clinicalMeaning = when {
        modal.name.contains("Pressure", ignoreCase = true) || modal.name.equals("BP", ignoreCase = true) ->
            "Blood pressure measures arterial tension during cardiac contraction (systolic) and resting diastole. Keeping BP within target limits protects against cardiovascular events and preserves renal microvasculature."
        modal.name.contains("Sugar", ignoreCase = true) || modal.name.contains("Glucose", ignoreCase = true) || modal.name.contains("FBS", ignoreCase = true) ->
            "Blood glucose indicates carbohydrate metabolism and insulin sensitivity. Well-controlled levels prevent microvascular damage to eyes, kidneys, and peripheral nerves."
        modal.name.contains("Heart", ignoreCase = true) || modal.name.contains("Pulse", ignoreCase = true) ->
            "Resting pulse rate evaluates cardiac rhythm stability, autonomic tone, and cardiovascular fitness. Regular aerobic exercise supports lower resting heart rates."
        modal.name.contains("SpO2", ignoreCase = true) || modal.name.contains("Oxygen", ignoreCase = true) ->
            "SpO2 measures the percentage of hemoglobin carrying oxygen. Maintaining readings above 95% ensures organs and skeletal muscles receive optimal oxygen delivery."
        modal.name.contains("Creatinine", ignoreCase = true) ->
            "Serum creatinine is a waste product filtered by the kidneys. Normal values confirm healthy glomerular filtration rate (eGFR)."
        modal.name.contains("HbA1c", ignoreCase = true) ->
            "HbA1c measures the percentage of glycated hemoglobin, providing a 3-month retrospective index of glycemic control without daily fluctuation bias."
        modal.name.contains("Hemoglobin", ignoreCase = true) || modal.name.equals("Hb", ignoreCase = true) ->
            "Hemoglobin is the vital protein in red blood cells that transports oxygen from your lungs throughout the body. Healthy levels prevent fatigue and anemia."
        else ->
            "This biomarker was extracted from your diagnostic laboratory reports. Tracking it over time provides your healthcare provider with actionable clinical trajectory data."
    }

    val isLoggableVital = modal.name.lowercase() in listOf("blood pressure", "bp", "weight")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("biomarker_detail_dialog"),
        icon = {
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            modal.name.contains("Pressure", ignoreCase = true) || modal.name.equals("BP", ignoreCase = true) -> Icons.Default.Speed
                            modal.name.contains("Sugar", ignoreCase = true) || modal.name.contains("Glucose", ignoreCase = true) -> Icons.Default.Science
                            modal.name.contains("Heart", ignoreCase = true) || modal.name.contains("Pulse", ignoreCase = true) -> Icons.Default.Favorite
                            modal.name.contains("SpO2", ignoreCase = true) || modal.name.contains("Oxygen", ignoreCase = true) -> Icons.Default.Air
                            else -> Icons.Default.HealthAndSafety
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = modal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = organSystemName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Value & Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT MEASUREMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = valueStr,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Reference Range Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reference Range",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = referenceRange,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Clinical Meaning & Plain Language Guide
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Clinical Significance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = clinicalMeaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            if (isLoggableVital) {
                Button(
                    onClick = onLogVital,
                    modifier = Modifier.testTag("biomarker_log_reading_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Log Vital")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (isLoggableVital) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun AiHealthSummaryCard(
    state: AiHealthSummaryUiState,
    onSelectScope: (AiSummaryScope, Long?) -> Unit = { _, _ -> },
    onRefresh: () -> Unit,
    onOpenSafetyAudit: () -> Unit,
    onOpenDoctorBrief: () -> Unit,
    onOpenLabExplainer: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_health_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: AI Branding + Timestamp + Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.home_ai_summary_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.lastAnalyzedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(id = R.string.home_ai_refresh),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Scope Selector Chips (Total Health vs Specific Report)
            if (state.availableReports.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedScope == AiSummaryScope.TOTAL_HEALTH,
                        onClick = { onSelectScope(AiSummaryScope.TOTAL_HEALTH, null) },
                        label = { Text("Total Health") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    state.availableReports.forEach { report ->
                        val isSelected = state.selectedScope == AiSummaryScope.SPECIFIC_REPORT && state.selectedReportId == report.recordId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectScope(AiSummaryScope.SPECIFIC_REPORT, report.recordId) },
                            label = { Text(report.title.take(18)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (report.isPrescription) Icons.Default.Medication else Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Health Score & Stability Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.home_ai_score_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${state.healthScore}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " / 100",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        // Status Pill Badge
                        val pillColor = when (state.statusTier) {
                            HealthStatusTier.OPTIMAL -> MaterialTheme.colorScheme.primary
                            HealthStatusTier.STABLE -> MaterialTheme.colorScheme.tertiary
                            HealthStatusTier.ATTENTION_NEEDED -> MaterialTheme.colorScheme.error
                        }
                        val pillText = when (state.statusTier) {
                            HealthStatusTier.OPTIMAL -> stringResource(id = R.string.home_ai_pill_optimal)
                            HealthStatusTier.STABLE -> stringResource(id = R.string.home_ai_pill_stable)
                            HealthStatusTier.ATTENTION_NEEDED -> stringResource(id = R.string.home_ai_pill_attention)
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = pillColor
                        ) {
                            Text(
                                text = pillText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Medication Adherence Progress Bar
                    if (state.totalMedications > 0) {
                        val progress = state.takenMedications.toFloat() / state.totalMedications.toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Today's Intake: ${state.takenMedications} of ${state.totalMedications} doses taken",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // Key Observations List
            if (state.insights.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.home_ai_sec_observations),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    state.insights.forEach { insight ->
                        AiInsightRow(insight = insight)
                    }
                }
            }

            // Action Care Guidance (if available)
            if (state.actionRecommendations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.home_ai_sec_actions),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    state.actionRecommendations.forEach { rec ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Interactive AI Action Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenSafetyAudit,
                    label = { Text(stringResource(id = R.string.home_ai_chip_interaction)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                )

                AssistChip(
                    onClick = onOpenDoctorBrief,
                    label = { Text(stringResource(id = R.string.home_ai_chip_brief)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )

                AssistChip(
                    onClick = onOpenLabExplainer,
                    label = { Text(stringResource(id = R.string.home_ai_chip_explainer)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
private fun AiInsightRow(
    insight: AiInsightItem,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            val (icon, iconTint) = when (insight.category) {
                AiInsightCategory.MEDICATION -> Pair(Icons.Default.Medication, MaterialTheme.colorScheme.primary)
                AiInsightCategory.LAB_VITAL -> Pair(Icons.Default.ShowChart, MaterialTheme.colorScheme.tertiary)
                AiInsightCategory.ALLERGY_SAFETY -> Pair(Icons.Default.Shield, MaterialTheme.colorScheme.error)
                AiInsightCategory.CARE_ADVICE -> Pair(Icons.Default.Favorite, MaterialTheme.colorScheme.secondary)
            }

            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LabExplainerItem(
    title: String,
    normalRange: String,
    meaning: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Target / Normal: $normalRange",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExtractedInformationSection(
    extractedInfos: List<ExtractedMedicalInfo>,
    biomarkers: List<ParsedMedicalResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extracted Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${biomarkers.size} Extracted Markers",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            val latestDoc = extractedInfos.maxByOrNull { it.updatedAtTimestamp }
            if (latestDoc != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source: ${latestDoc.documentTitle} • ${latestDoc.reportDate ?: "Recent"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                biomarkers.forEach { biomarker ->
                    ExtractedBiomarkerRow(biomarker = biomarker)
                }
            }
        }
    }
}

@Composable
private fun ExtractedBiomarkerRow(biomarker: ParsedMedicalResult) {
    val status = biomarker.status
    val statusColor = when (status) {
        BiomarkerStatus.CRITICAL_HIGH, BiomarkerStatus.CRITICAL_LOW -> androidx.compose.ui.graphics.Color(0xFFEF4444)
        BiomarkerStatus.HIGH, BiomarkerStatus.LOW, BiomarkerStatus.QUALITATIVE_ABNORMAL -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
        BiomarkerStatus.NORMAL -> androidx.compose.ui.graphics.Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.primary
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            if (biomarker.isOutOfRange) 1.dp else 0.5.dp,
            if (biomarker.isOutOfRange) statusColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = biomarker.testName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (statusBadge.isNotEmpty()) {
                        Text(
                            text = statusBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                    }
                }
                if (biomarker.referenceRange.isNotBlank()) {
                    Text(
                        text = "Ref: ${biomarker.referenceRange}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${biomarker.value} ${biomarker.unit}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (biomarker.isOutOfRange) statusColor else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun HomeMedicationItemRow(
    medication: PrescriptionMedicationEntity,
    onToggleTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val guidance = remember(medication.name) { MedicationAdvisoryHelper.getGuidance(medication.name) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_medication_row_${medication.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (medication.isTaken) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            if (medication.isTaken) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (medication.isTaken) Icons.Default.CheckCircle else Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (medication.isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (medication.isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medication.dosage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = medication.timeSlot,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onToggleTaken,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (medication.isTaken) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (medication.isTaken) "Taken ✓" else "Take",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (guidance != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💡 ${guidance.foodInstruction}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

