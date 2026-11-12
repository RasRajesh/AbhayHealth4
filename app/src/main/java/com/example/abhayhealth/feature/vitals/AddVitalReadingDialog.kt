package com.example.abhayhealth.feature.vitals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.abhayhealth.R

@Composable
fun AddVitalReadingDialog(
    initialMetric: VitalType,
    onDismissRequest: () -> Unit,
    onSaveReading: (type: VitalType, systolic: Double?, diastolic: Double?, weightKg: Double?, notes: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialMetric) }
    var systolicInput by remember { mutableStateOf("") }
    var diastolicInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("dialog_add_vital"),
        title = {
            Text(
                text = stringResource(id = R.string.vitals_dialog_title),
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
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == VitalType.BLOOD_PRESSURE,
                        onClick = {
                            selectedType = VitalType.BLOOD_PRESSURE
                            errorMessage = null
                        },
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
                        modifier = Modifier.testTag("dialog_chip_bp")
                    )

                    FilterChip(
                        selected = selectedType == VitalType.WEIGHT,
                        onClick = {
                            selectedType = VitalType.WEIGHT
                            errorMessage = null
                        },
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
                        modifier = Modifier.testTag("dialog_chip_weight")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedType) {
                    VitalType.BLOOD_PRESSURE -> {
                        OutlinedTextField(
                            value = systolicInput,
                            onValueChange = {
                                systolicInput = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(id = R.string.vitals_dialog_systolic)) },
                            placeholder = { Text("e.g. 120") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_systolic")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = diastolicInput,
                            onValueChange = {
                                diastolicInput = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(id = R.string.vitals_dialog_diastolic)) },
                            placeholder = { Text("e.g. 80") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_diastolic")
                        )
                    }
                    VitalType.WEIGHT -> {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(id = R.string.vitals_dialog_weight)) },
                            placeholder = { Text("e.g. 70.5") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_weight")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(id = R.string.vitals_dialog_notes)) },
                    placeholder = { Text("e.g. Morning resting measurement") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_vital_notes")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedType) {
                        VitalType.BLOOD_PRESSURE -> {
                            val sys = systolicInput.toDoubleOrNull()
                            val dia = diastolicInput.toDoubleOrNull()
                            if (sys == null || sys !in 50.0..300.0) {
                                errorMessage = "Please enter a valid systolic pressure (50–300 mmHg)."
                                return@Button
                            }
                            if (dia == null || dia !in 30.0..200.0) {
                                errorMessage = "Please enter a valid diastolic pressure (30–200 mmHg)."
                                return@Button
                            }
                            onSaveReading(selectedType, sys, dia, null, notesInput.trim())
                        }
                        VitalType.WEIGHT -> {
                            val w = weightInput.toDoubleOrNull()
                            if (w == null || w !in 15.0..400.0) {
                                errorMessage = "Please enter a valid weight (15–400 kg)."
                                return@Button
                            }
                            onSaveReading(selectedType, null, null, w, notesInput.trim())
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_save_vital")
            ) {
                Text(stringResource(id = R.string.vitals_dialog_save))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_cancel_vital")
            ) {
                Text(stringResource(id = R.string.vitals_dialog_cancel))
            }
        }
    )
}
