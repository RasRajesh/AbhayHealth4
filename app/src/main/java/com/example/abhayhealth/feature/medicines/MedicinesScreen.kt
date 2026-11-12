package com.example.abhayhealth.feature.medicines

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.abhayhealth.R
import com.example.abhayhealth.core.designsystem.components.AbhayHealthBrandLogo
import com.example.abhayhealth.core.designsystem.components.EmptyStateContent

@Composable
fun MedicinesScreen(
    viewModel: MedicinesViewModel,
    modifier: Modifier = Modifier
) {
    val medications by viewModel.medications.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showRemindersDialog by viewModel.showRemindersDialog.collectAsState()
    val editingMedication by viewModel.editingMedication.collectAsState()

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { viewModel.closeAddDialog() },
            onAdd = { name, dosage, slot ->
                viewModel.addMedication(name, dosage, slot)
            }
        )
    }

    editingMedication?.let { med ->
        EditMedicationDialog(
            medication = med,
            onDismiss = { viewModel.closeEditDialog() },
            onSave = { name, dosage, slot ->
                viewModel.updateMedication(med.id, name, dosage, slot)
            }
        )
    }

    if (showRemindersDialog) {
        MedicationRemindersDialog(
            medications = medications,
            onDismiss = { viewModel.closeRemindersDialog() },
            onEditMedication = { med ->
                viewModel.openEditDialog(med)
            },
            onSendTestNotification = {
                viewModel.triggerTestReminder()
            },
            onRescheduleAll = {
                viewModel.rescheduleAllAlarms()
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        text = stringResource(id = R.string.meds_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.meds_subtitle),
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
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.openAddDialog() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("meds_add_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.meds_btn_add),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.openRemindersDialog() },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("meds_reminders_button")
                ) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.meds_btn_reminders),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(id = R.string.meds_section_schedule),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            MedicationDailyTimelineGraph(
                medicationList = medications,
                onToggleTaken = { id -> viewModel.toggleDoseTaken(id) }
            )
        }

        if (medications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    EmptyStateContent(
                        title = stringResource(id = R.string.meds_empty_schedule_title),
                        description = stringResource(id = R.string.meds_empty_schedule_desc),
                        icon = Icons.Default.Medication,
                        actionLabel = stringResource(id = R.string.meds_btn_add),
                        onActionClick = { viewModel.openAddDialog() }
                    )
                }
            }
        } else {
            val distinctSlots = medications.map { it.timeSlot }.distinct().sortedBy { slot ->
                val (h, m) = MedicationAlarmScheduler.parseSlotTime(slot)
                h * 60 + m
            }
            distinctSlots.forEach { slot ->
                val slotMeds = medications.filter { it.timeSlot == slot }
                if (slotMeds.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(
                        items = slotMeds,
                        key = { it.id }
                    ) { med ->
                        MedicationCard(
                            medication = med,
                            onToggle = { viewModel.toggleDoseTaken(med.id) },
                            onEdit = { viewModel.openEditDialog(med) },
                            onDelete = { viewModel.removeMedication(med.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medication: PrescriptionMedication,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val guidance = remember(medication.name) { MedicationAdvisoryHelper.getGuidance(medication.name) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.isTaken) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (medication.isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (medication.isTaken) "Mark pending" else "Mark taken",
                        tint = if (medication.isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
                ) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = medication.timeSlot,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("med_edit_btn_${medication.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.meds_edit_reminder_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("med_delete_btn_${medication.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete medication",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (guidance != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Column {
                            Text(
                                text = "${guidance.tip} • ${guidance.foodInstruction}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (guidance.cautionaryNote != null) {
                                Text(
                                    text = guidance.cautionaryNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderTimeEditor(
    selectedTimeSlot: String,
    onTimeSlotChanged: (String) -> Unit
) {
    val standardSlots = listOf(
        "MORNING (08:30)",
        "AFTERNOON (13:30)",
        "EVENING (19:00)",
        "NIGHT (21:30)"
    )

    val (parsedH, parsedM) = remember(selectedTimeSlot) {
        MedicationAlarmScheduler.parseSlotTime(selectedTimeSlot)
    }

    var isCustom by remember(selectedTimeSlot) {
        mutableStateOf(standardSlots.none { it.equals(selectedTimeSlot, ignoreCase = true) })
    }

    var hour12 by remember(parsedH) {
        mutableStateOf(if (parsedH == 0) 12 else if (parsedH > 12) parsedH - 12 else parsedH)
    }
    var minute by remember(parsedM) {
        mutableStateOf(parsedM)
    }
    var isAm by remember(parsedH) {
        mutableStateOf(parsedH < 12)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.meds_edit_time_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        // Switch between Preset slots and Custom time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !isCustom,
                onClick = {
                    isCustom = false
                    val defaultPreset = standardSlots.firstOrNull { it.equals(selectedTimeSlot, ignoreCase = true) } ?: standardSlots[0]
                    onTimeSlotChanged(defaultPreset)
                },
                label = { Text(stringResource(id = R.string.meds_quick_slots)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = isCustom,
                onClick = {
                    isCustom = true
                    val h24 = if (isAm) (if (hour12 == 12) 0 else hour12) else (if (hour12 == 12) 12 else hour12 + 12)
                    onTimeSlotChanged(MedicationAlarmScheduler.formatTimeLabel(h24, minute))
                },
                label = { Text(stringResource(id = R.string.meds_custom_time_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (!isCustom) {
            // Preset Slots List
            standardSlots.forEach { slot ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTimeSlotChanged(slot) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedTimeSlot.equals(slot, ignoreCase = true),
                        onClick = { onTimeSlotChanged(slot) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slot,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedTimeSlot.equals(slot, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        } else {
            // Interactive Custom Time Selector Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val h24 = if (isAm) (if (hour12 == 12) 0 else hour12) else (if (hour12 == 12) 12 else hour12 + 12)
                    val formatted = MedicationAlarmScheduler.formatTimeLabel(h24, minute)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder Alarm Time:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // AM / PM Toggle Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isAm,
                            onClick = {
                                isAm = true
                                val newH24 = if (hour12 == 12) 0 else hour12
                                onTimeSlotChanged(MedicationAlarmScheduler.formatTimeLabel(newH24, minute))
                            },
                            label = { Text("AM (Morning)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isAm,
                            onClick = {
                                isAm = false
                                val newH24 = if (hour12 == 12) 12 else hour12 + 12
                                onTimeSlotChanged(MedicationAlarmScheduler.formatTimeLabel(newH24, minute))
                            },
                            label = { Text("PM (Afternoon/Night)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Hour Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hour:", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val newH = if (hour12 <= 1) 12 else hour12 - 1
                                    hour12 = newH
                                    val newH24 = if (isAm) (if (newH == 12) 0 else newH) else (if (newH == 12) 12 else newH + 12)
                                    onTimeSlotChanged(MedicationAlarmScheduler.formatTimeLabel(newH24, minute))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format(java.util.Locale.US, "%02d", hour12),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    val newH = if (hour12 >= 12) 1 else hour12 + 1
                                    hour12 = newH
                                    val newH24 = if (isAm) (if (newH == 12) 0 else newH) else (if (newH == 12) 12 else newH + 12)
                                    onTimeSlotChanged(MedicationAlarmScheduler.formatTimeLabel(newH24, minute))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Minute Quick Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Minute:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0, 15, 30, 45).forEach { m ->
                                FilterChip(
                                    selected = minute == m,
                                    onClick = {
                                        minute = m
                                        val curH24 = if (isAm) (if (hour12 == 12) 0 else hour12) else (if (hour12 == 12) 12 else hour12 + 12)
                                        onTimeSlotChanged(MedicationAlarmScheduler.formatTimeLabel(curH24, m))
                                    },
                                    label = { Text(String.format(java.util.Locale.US, ":%02d", m)) },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedSlot by remember { mutableStateOf("MORNING (08:30)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.meds_btn_add)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    label = { Text("Dosage (e.g. 500 mg - 1 Tablet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ReminderTimeEditor(
                    selectedTimeSlot = selectedSlot,
                    onTimeSlotChanged = { selectedSlot = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, dosage, selectedSlot) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(id = R.string.meds_btn_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun EditMedicationDialog(
    medication: PrescriptionMedication,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember(medication) { mutableStateOf(medication.name) }
    var dosage by remember(medication) { mutableStateOf(medication.dosage) }
    var selectedSlot by remember(medication) { mutableStateOf(medication.timeSlot) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(id = R.string.meds_edit_reminder_title))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    label = { Text("Dosage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ReminderTimeEditor(
                    selectedTimeSlot = selectedSlot,
                    onTimeSlotChanged = { selectedSlot = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, dosage, selectedSlot) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(id = R.string.meds_save_reminder))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun MedicationRemindersDialog(
    medications: List<PrescriptionMedication>,
    onDismiss: () -> Unit,
    onEditMedication: (PrescriptionMedication) -> Unit,
    onSendTestNotification: () -> Unit,
    onRescheduleAll: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val defaultSlots = listOf(
        Triple("MORNING (08:30)", "Morning Dose", "08:30 AM"),
        Triple("AFTERNOON (13:30)", "Afternoon Dose", "01:30 PM"),
        Triple("EVENING (19:00)", "Evening Dose", "07:00 PM"),
        Triple("NIGHT (21:30)", "Night Dose", "09:30 PM")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.meds_reminders_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = stringResource(id = R.string.meds_reminders_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Permission Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasPermission) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (hasPermission) {
                                        stringResource(id = R.string.meds_reminders_notif_enabled)
                                    } else {
                                        stringResource(id = R.string.meds_reminders_notif_disabled)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(id = R.string.meds_reminders_btn_grant))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Daily Reminder Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Standard schedule slots list
                items(defaultSlots) { (slotKey, slotName, slotTime) ->
                    val medsInSlot = medications.filter { it.timeSlot.equals(slotKey, ignoreCase = true) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = slotName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = slotTime,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            if (medsInSlot.isEmpty()) {
                                Text(
                                    text = "No medicines scheduled for this slot",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            } else {
                                medsInSlot.forEach { med ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${med.name} (${med.dosage})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                onDismiss()
                                                onEditMedication(med)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(id = R.string.meds_edit_reminder_title),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Scheduled Slots
                val customSlotMeds = medications.filter { med ->
                    defaultSlots.none { it.first.equals(med.timeSlot, ignoreCase = true) }
                }
                if (customSlotMeds.isNotEmpty()) {
                    item {
                        Text(
                            text = "Custom Scheduled Alarms",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(customSlotMeds) { med ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = med.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${med.dosage} • ${med.timeSlot}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onEditMedication(med)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(id = R.string.meds_edit_reminder_title),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Test Notification & Reschedule actions
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestNotification()
                                Toast.makeText(context, context.getString(R.string.meds_reminders_test_success), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.meds_reminders_btn_test))
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            onRescheduleAll()
                            Toast.makeText(context, context.getString(R.string.meds_reminders_sync_success), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.meds_reminders_btn_sync))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.common_close))
            }
        }
    )
}
