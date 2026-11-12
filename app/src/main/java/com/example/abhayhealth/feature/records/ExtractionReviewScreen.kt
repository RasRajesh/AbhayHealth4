package com.example.abhayhealth.feature.records

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import com.example.abhayhealth.feature.medicines.PrescriptionMedicationEntity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.abhayhealth.R
import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.domain.model.BiomarkerStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionReviewScreen(
    viewModel: ExtractionReviewViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val record by viewModel.selectedRecord.collectAsState()
    val extractedInfo by viewModel.extractedInfo.collectAsState()
    val title by viewModel.documentTitle.collectAsState()
    val date by viewModel.reportDate.collectAsState()
    val testResults by viewModel.parsedResults.collectAsState()
    val parsedMedicines by viewModel.parsedMedicines.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    val hasExtractionFailed by viewModel.hasExtractionFailed.collectAsState()
    val verifiedTestIds by viewModel.verifiedTestIds.collectAsState()
    val verifiedMedicineIds by viewModel.verifiedMedicineIds.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val showReExtractWarning by viewModel.showReExtractWarning.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to Extracted Info tab
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddMedDialog by remember { mutableStateOf(false) }
    var editingTestResult by remember { mutableStateOf<ParsedMedicalResult?>(null) }
    var editingMedicine by remember { mutableStateOf<PrescriptionMedicationEntity?>(null) }
    var showRawText by remember { mutableStateOf(false) }

    if (showReExtractWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelReExtraction() },
            title = { Text("Re-run Document Extraction?") },
            text = { Text("Re-running extraction will overwrite your previously saved and edited test values with fresh OCR output. Continue?") },
            confirmButton = {
                Button(onClick = { viewModel.confirmReExtraction() }) {
                    Text("Re-extract")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelReExtraction() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AddTestResultDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { testName, value, unit, refRange ->
                viewModel.addTestResult(testName, value, unit, refRange)
                showAddDialog = false
            }
        )
    }

    if (editingTestResult != null) {
        val currentEdit = editingTestResult!!
        EditTestResultDialog(
            result = currentEdit,
            onDismiss = { editingTestResult = null },
            onSave = { updatedName, updatedVal, updatedUnit, updatedRange ->
                viewModel.updateTestResult(currentEdit.id, updatedName, updatedVal, updatedUnit, updatedRange)
                editingTestResult = null
            }
        )
    }

    if (showAddMedDialog) {
        val currentUid = record?.accountUid ?: "local_user"
        AddMedicineDialog(
            onDismiss = { showAddMedDialog = false },
            onAdd = { name, dosage, timeSlot ->
                viewModel.addMedicine(
                    PrescriptionMedicationEntity(
                        accountUid = currentUid,
                        name = name,
                        dosage = dosage.ifBlank { "As Directed" },
                        timeSlot = timeSlot.ifBlank { "MORNING (08:30)" }
                    )
                )
                showAddMedDialog = false
            }
        )
    }

    if (editingMedicine != null) {
        val currentMed = editingMedicine!!
        EditMedicineDialog(
            medicine = currentMed,
            onDismiss = { editingMedicine = null },
            onSave = { updatedName, updatedDosage, updatedSlot ->
                viewModel.updateMedicine(
                    currentMed.copy(
                        name = updatedName,
                        dosage = updatedDosage,
                        timeSlot = updatedSlot
                    )
                )
                editingMedicine = null
            }
        )
    }

    if (isExtracting) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelExtraction() },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelExtraction() }) {
                    Text("Cancel Extraction")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Extracting Health Data...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Analyzing report text, recognizing biomarker measurements, and extracting prescribed medicines. Please wait...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = record?.title ?: "Document Review") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.common_back)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Original Document") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Extracted Info") }
                )
            }

            if (selectedTabIndex == 0) {
                // Tab 1: Original Document View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val path = record?.localFilePath
                    val isImage = record?.mimeType?.contains("image", ignoreCase = true) == true

                    if (path != null && isImage && File(path).exists()) {
                        val bitmap = remember(path) {
                            try {
                                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(path, bounds)
                                val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
                                var sample = 1
                                while (maxDim / sample > 1024) {
                                    sample *= 2
                                }
                                val opts = BitmapFactory.Options().apply {
                                    inSampleSize = sample
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                }
                                BitmapFactory.decodeFile(path, opts)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Original Medical Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Medical Image Attached",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Original Document (${record?.mimeType ?: "PDF"})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                record?.let { DocumentImportManager.openRecord(it, context, it.accountUid) }
                            }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Original File")
                        }
                    }
                }
            } else {
                // Tab 2: Extracted Information & Review
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Badge Card
                    item {
                        val statusText = when (extractedInfo?.reviewStatus) {
                            ExtractedMedicalInfo.STATUS_REVIEWED_AND_SAVED -> "Reviewed & Saved"
                            ExtractedMedicalInfo.STATUS_EDITED -> "Patient Edited"
                            else -> "Auto-Extracted (Pending Review)"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Status: $statusText",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Mandatory Persistent Review Notice Banner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Automatically extracted information may contain errors. Compare it with your original report and review all values before saving.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    if (hasExtractionFailed || errorMessage != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Extraction Issue Detected",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.clearMessages() }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = errorMessage ?: "No medical test results or medications were automatically recognized in this document. You can retry the extraction or add fields manually.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.retryExtraction() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry Extraction")
                                        }
                                        OutlinedButton(
                                            onClick = { showAddDialog = true },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Field")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (infoMessage != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = infoMessage!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.clearMessages() }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }

                    // Verification Progress Summary Card
                    val totalFields = testResults.size + parsedMedicines.size
                    val verifiedFields = verifiedTestIds.size + verifiedMedicineIds.size
                    val allVerified = totalFields > 0 && verifiedFields >= totalFields

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (allVerified) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (allVerified) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (allVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (allVerified) "All Items Verified ✓" else "Verification Progress ($verifiedFields / $totalFields)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (totalFields > 0 && !allVerified) {
                                        TextButton(onClick = { viewModel.verifyAll() }) {
                                            Text("Verify All")
                                        }
                                    }
                                }
                                if (totalFields > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { verifiedFields.toFloat() / totalFields.toFloat() },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Editable Metadata
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { viewModel.onTitleChanged(it) },
                                label = { Text("Report Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = date,
                                onValueChange = { viewModel.onDateChanged(it) },
                                label = { Text("Report Date") },
                                placeholder = { Text("e.g. 15/08/2025") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Prescribed Medications Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Prescribed Medicines (${parsedMedicines.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(onClick = { showAddMedDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Med")
                            }
                        }
                    }

                    if (parsedMedicines.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "No prescribed medicines recognized in this report. Tap '+ Add Med' to add prescriptions to your medicine schedule.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = parsedMedicines,
                            key = { it.id }
                        ) { med ->
                            val isVerified = verifiedMedicineIds.contains(med.id.toString())
                            MedicineResultCard(
                                medicine = med,
                                isVerified = isVerified,
                                onToggleVerified = { viewModel.toggleMedicineVerified(med.id.toString()) },
                                onEditModal = { editingMedicine = med },
                                onUpdate = { updatedMed -> viewModel.updateMedicine(updatedMed) },
                                onDelete = { viewModel.deleteMedicine(med.id) }
                            )
                        }
                    }

                    // Extracted Test Results List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recognized Test Results (${testResults.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(onClick = { showAddDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Field")
                            }
                        }
                    }

                    if (testResults.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "No structured test results recognized automatically. You can add test fields manually or view raw text.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = testResults,
                            key = { it.id }
                        ) { result ->
                            val isVerified = verifiedTestIds.contains(result.id)
                            TestResultCard(
                                result = result,
                                isVerified = isVerified,
                                onToggleVerified = { viewModel.toggleTestVerified(result.id) },
                                onEditModal = { editingTestResult = result },
                                onUpdate = { name, valStr, unit, range ->
                                    viewModel.updateTestResult(result.id, name, valStr, unit, range)
                                },
                                onDelete = { viewModel.deleteTestResult(result.id) }
                            )
                        }
                    }

                    // Collapsible Raw Text Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Raw Extracted OCR Text",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(onClick = { showRawText = !showRawText }) {
                                        Text(if (showRawText) "Hide" else "Show")
                                    }
                                }

                                if (showRawText) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = extractedInfo?.rawText ?: "No raw text extracted.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Re-extract and Save Actions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.requestReExtraction() },
                                enabled = !isExtracting && !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-extract")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveReviewedInfo(context = context, onSuccess = onBackClick)
                                },
                                enabled = !isExtracting && !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text("Save Reviewed Info", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestResultCard(
    result: ParsedMedicalResult,
    isVerified: Boolean,
    onToggleVerified: () -> Unit,
    onEditModal: () -> Unit,
    onUpdate: (String, String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    val status = result.status
    val statusColor = when (status) {
        BiomarkerStatus.CRITICAL_HIGH, BiomarkerStatus.CRITICAL_LOW -> Color(0xFFEF4444)
        BiomarkerStatus.HIGH, BiomarkerStatus.LOW, BiomarkerStatus.QUALITATIVE_ABNORMAL -> Color(0xFFF59E0B)
        BiomarkerStatus.NORMAL -> Color(0xFF10B981)
        else -> Color.Transparent
    }
    val statusLabel = when (status) {
        BiomarkerStatus.CRITICAL_HIGH -> "CRITICAL HIGH ▲"
        BiomarkerStatus.CRITICAL_LOW -> "CRITICAL LOW ▼"
        BiomarkerStatus.HIGH -> "HIGH ▲"
        BiomarkerStatus.LOW -> "LOW ▼"
        BiomarkerStatus.QUALITATIVE_ABNORMAL -> "ABNORMAL ⚠"
        BiomarkerStatus.NORMAL -> "NORMAL ✓"
        else -> ""
    }

    val isIncomplete = result.testName.isBlank() || result.value.isBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isVerified) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isIncomplete) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        else if (isVerified) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Status Chip, Incomplete Badge, Verification Chip, Edit & Delete actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (statusLabel.isNotEmpty()) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isIncomplete) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "⚠️ INCOMPLETE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = isVerified,
                        onClick = onToggleVerified,
                        label = { Text(if (isVerified) "Verified ✓" else "Verify", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (isVerified) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )

                    IconButton(onClick = onEditModal, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit field details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete field",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (result.referenceRange.isNotBlank()) {
                Text(
                    text = "Reference Range: ${result.referenceRange}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Editable Input Fields
            OutlinedTextField(
                value = result.testName,
                onValueChange = { onUpdate(it, result.value, result.unit, result.referenceRange) },
                label = { Text("Test Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = result.value,
                    onValueChange = { onUpdate(result.testName, it, result.unit, result.referenceRange) },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = result.unit,
                    onValueChange = { onUpdate(result.testName, result.value, it, result.referenceRange) },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = result.referenceRange,
                    onValueChange = { onUpdate(result.testName, result.value, result.unit, it) },
                    label = { Text("Ref Range") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun AddTestResultDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var range by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medical Test Result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Test Name (e.g. Hemoglobin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value (e.g. 13.5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g. g/dL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = range,
                    onValueChange = { range = it },
                    label = { Text("Reference Range (e.g. 12.0 - 15.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, value, unit, range) },
                enabled = name.isNotBlank() && value.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditTestResultDialog(
    result: ParsedMedicalResult,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(result.testName) }
    var value by remember { mutableStateOf(result.value) }
    var unit by remember { mutableStateOf(result.unit) }
    var range by remember { mutableStateOf(result.referenceRange) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit & Verify Test Result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Test Name (e.g. Blood Glucose)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Measurement Value (e.g. 95)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g. mg/dL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = range,
                    onValueChange = { range = it },
                    label = { Text("Reference Range (e.g. 70 - 99)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, value, unit, range) },
                enabled = name.isNotBlank() && value.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MedicineResultCard(
    medicine: PrescriptionMedicationEntity,
    isVerified: Boolean,
    onToggleVerified: () -> Unit,
    onEditModal: () -> Unit,
    onUpdate: (PrescriptionMedicationEntity) -> Unit,
    onDelete: () -> Unit
) {
    val isIncomplete = medicine.name.isBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isVerified) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isIncomplete) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        else if (isVerified) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Verification Chip, Edit & Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = isVerified,
                        onClick = onToggleVerified,
                        label = { Text(if (isVerified) "Verified ✓" else "Verify", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (isVerified) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )

                    if (isIncomplete) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "⚠️ MISSING NAME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEditModal, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit medicine details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete medicine",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = medicine.name,
                onValueChange = { onUpdate(medicine.copy(name = it)) },
                label = { Text("Medicine Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = medicine.dosage,
                    onValueChange = { onUpdate(medicine.copy(dosage = it)) },
                    label = { Text("Dosage (e.g. 500mg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = medicine.timeSlot,
                    onValueChange = { onUpdate(medicine.copy(timeSlot = it)) },
                    label = { Text("Time Slot") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, dosage: String, timeSlot: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("MORNING (08:30)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Prescribed Medicine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name (e.g. Paracetamol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 500 mg or 1 tablet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Slot (e.g. MORNING (08:30))") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, dosage, timeSlot) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditMedicineDialog(
    medicine: PrescriptionMedicationEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, timeSlot: String) -> Unit
) {
    var name by remember { mutableStateOf(medicine.name) }
    var dosage by remember { mutableStateOf(medicine.dosage) }
    var timeSlot by remember { mutableStateOf(medicine.timeSlot) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit & Verify Medicine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 500 mg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Slot (e.g. MORNING (08:30))") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, dosage, timeSlot) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
