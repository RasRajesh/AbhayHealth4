package com.example.abhayhealth.feature.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File
import com.example.abhayhealth.R
import com.example.abhayhealth.core.designsystem.components.AbhayHealthTopAppBar
import com.example.abhayhealth.core.designsystem.components.AbhayPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: ProfileViewModel,
    userPhone: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullName by viewModel.fullName.collectAsState()
    val dateOfBirth by viewModel.dateOfBirth.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val city by viewModel.city.collectAsState()
    val state by viewModel.state.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val bloodGroup by viewModel.bloodGroup.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val healthConditions by viewModel.healthConditions.collectAsState()
    val emergencyContactName by viewModel.emergencyContactName.collectAsState()
    val emergencyContactPhone by viewModel.emergencyContactPhone.collectAsState()
    val additionalNotes by viewModel.additionalNotes.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val validationError by viewModel.validationError.collectAsState()

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.onPhotoSelected(uri, context)
    }

    val profileBitmap = remember(photoUri) {
        val uriStr = photoUri
        if (uriStr.isNullOrBlank()) null
        else {
            try {
                if (uriStr.startsWith("/")) {
                    val file = File(uriStr)
                    if (file.exists()) BitmapFactory.decodeFile(uriStr) else null
                } else {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AbhayHealthTopAppBar(
                title = stringResource(id = R.string.profile_edit_title),
                onBackClick = onBackClick,
                showBrandLogo = true
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Photo & Phone Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Text(text = if (photoUri != null) "Change Photo" else "Add Photo")
                        }
                        if (photoUri != null) {
                            TextButton(onClick = { viewModel.removePhoto() }) {
                                Text(
                                    text = "Remove",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (!userPhone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = maskPhoneNumber(userPhone),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 1: Personal Details
            Text(
                text = stringResource(id = R.string.profile_sec_personal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { viewModel.onFullNameChanged(it) },
                label = { Text(stringResource(id = R.string.profile_full_name_label)) },
                placeholder = { Text(stringResource(id = R.string.profile_full_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = validationError != null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { viewModel.onDateOfBirthChanged(it) },
                    label = { Text(stringResource(id = R.string.profile_dob_label)) },
                    placeholder = { Text("15/08/1990") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = gender,
                    onValueChange = { viewModel.onGenderChanged(it) },
                    label = { Text(stringResource(id = R.string.profile_gender_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { viewModel.onCityChanged(it) },
                    label = { Text(stringResource(id = R.string.profile_city_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state,
                    onValueChange = { viewModel.onStateChanged(it) },
                    label = { Text(stringResource(id = R.string.profile_state_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = preferredLanguage,
                onValueChange = { viewModel.onLanguageChanged(it) },
                label = { Text(stringResource(id = R.string.profile_language_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Section 2: Baseline Health Information
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.profile_sec_health),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = bloodGroup,
                onValueChange = { viewModel.onBloodGroupChanged(it) },
                label = { Text(stringResource(id = R.string.profile_blood_group_label)) },
                placeholder = { Text("e.g. O+, A+, B-") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = allergies,
                onValueChange = { viewModel.onAllergiesChanged(it) },
                label = { Text(stringResource(id = R.string.profile_allergies_label)) },
                placeholder = { Text(stringResource(id = R.string.profile_allergies_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = healthConditions,
                onValueChange = { viewModel.onHealthConditionsChanged(it) },
                label = { Text(stringResource(id = R.string.profile_conditions_label)) },
                placeholder = { Text(stringResource(id = R.string.profile_conditions_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = emergencyContactName,
                    onValueChange = { viewModel.onEmergencyContactNameChanged(it) },
                    label = { Text(stringResource(id = R.string.profile_emergency_name_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = emergencyContactPhone,
                    onValueChange = { viewModel.onEmergencyContactPhoneChanged(it) },
                    label = { Text(stringResource(id = R.string.profile_emergency_phone_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { viewModel.onAdditionalNotesChanged(it) },
                label = { Text(stringResource(id = R.string.profile_notes_label)) },
                placeholder = { Text(stringResource(id = R.string.profile_notes_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Disclaimer Banner Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.profile_unverified_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
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
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.cancelEditing()
                        onBackClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.profile_btn_cancel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AbhayPrimaryButton(
                    text = stringResource(id = R.string.profile_btn_save),
                    onClick = {
                        viewModel.saveProfile(onSuccess = onBackClick)
                    },
                    enabled = !isSaving,
                    isLoading = isSaving,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }
        }
    }
}

private fun maskPhoneNumber(phone: String): String {
    if (phone.length < 10) return phone
    val prefix = phone.take(phone.length - 4)
    val suffix = phone.takeLast(4)
    val maskedPrefix = prefix.replace(Regex("\\d"), "*")
    return "$maskedPrefix$suffix"
}
