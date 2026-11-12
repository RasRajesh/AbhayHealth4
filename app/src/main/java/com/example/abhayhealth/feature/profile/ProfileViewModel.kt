package com.example.abhayhealth.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhayhealth.core.common.DefaultDispatcherProvider
import com.example.abhayhealth.core.common.DispatcherProvider
import com.example.abhayhealth.domain.model.PatientProfile
import com.example.abhayhealth.domain.repository.AuthRepository
import com.example.abhayhealth.domain.repository.PatientProfileRepository
import com.example.abhayhealth.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel(
    private val profileRepository: PatientProfileRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _dateOfBirth = MutableStateFlow("")
    val dateOfBirth: StateFlow<String> = _dateOfBirth.asStateFlow()

    private val _gender = MutableStateFlow("Prefer not to say")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state.asStateFlow()

    private val _preferredLanguage = MutableStateFlow("English")
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _photoUri = MutableStateFlow<String?>(null)
    val photoUri: StateFlow<String?> = _photoUri.asStateFlow()

    private val _bloodGroup = MutableStateFlow("")
    val bloodGroup: StateFlow<String> = _bloodGroup.asStateFlow()

    private val _allergies = MutableStateFlow("")
    val allergies: StateFlow<String> = _allergies.asStateFlow()

    private val _healthConditions = MutableStateFlow("")
    val healthConditions: StateFlow<String> = _healthConditions.asStateFlow()

    private val _emergencyContactName = MutableStateFlow("")
    val emergencyContactName: StateFlow<String> = _emergencyContactName.asStateFlow()

    private val _emergencyContactPhone = MutableStateFlow("")
    val emergencyContactPhone: StateFlow<String> = _emergencyContactPhone.asStateFlow()

    private val _additionalNotes = MutableStateFlow("")
    val additionalNotes: StateFlow<String> = _additionalNotes.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private var initialProfile: PatientProfile? = null

    init {
        loadProfileForCurrentUser()
    }

    fun loadProfileForCurrentUser() {
        val uid = authRepository.currentUser?.uid?.ifBlank { null } ?: "local_user"
        viewModelScope.launch(dispatcherProvider.main) {
            profileRepository.getProfile(uid).collect { profile ->
                if (profile != null) {
                    initialProfile = profile
                    populateFields(profile)
                }
            }
        }
    }

    private fun populateFields(profile: PatientProfile) {
        _fullName.value = profile.fullName
        _dateOfBirth.value = profile.dateOfBirth ?: ""
        _gender.value = profile.gender ?: "Prefer not to say"
        _city.value = profile.city ?: ""
        _state.value = profile.state ?: ""
        _preferredLanguage.value = profile.preferredLanguage
        _photoUri.value = profile.photoUri
        _bloodGroup.value = profile.bloodGroup ?: ""
        _allergies.value = profile.allergies ?: ""
        _healthConditions.value = profile.healthConditions ?: ""
        _emergencyContactName.value = profile.emergencyContactName ?: ""
        _emergencyContactPhone.value = profile.emergencyContactPhone ?: ""
        _additionalNotes.value = profile.additionalNotes ?: ""
    }

    fun onFullNameChanged(value: String) { _fullName.value = value; _validationError.value = null }
    fun onDateOfBirthChanged(value: String) { _dateOfBirth.value = value }
    fun onGenderChanged(value: String) { _gender.value = value }
    fun onCityChanged(value: String) { _city.value = value }
    fun onStateChanged(value: String) { _state.value = value }
    fun onLanguageChanged(value: String) { _preferredLanguage.value = value }

    fun onPhotoSelected(uri: Uri?, context: Context) {
        if (uri == null) {
            _photoUri.value = null
            return
        }
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val dir = File(context.filesDir, "profile_photos").apply { mkdirs() }
                val uid = authRepository.currentUser?.uid?.ifBlank { null } ?: "local_user"
                val photoFile = File(dir, "profile_${uid}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    photoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _photoUri.value = photoFile.absolutePath
            } catch (_: Exception) {
                _photoUri.value = uri.toString()
            }
        }
    }

    fun removePhoto() {
        _photoUri.value = null
    }

    fun onBloodGroupChanged(value: String) { _bloodGroup.value = value }
    fun onAllergiesChanged(value: String) { _allergies.value = value }
    fun onHealthConditionsChanged(value: String) { _healthConditions.value = value }
    fun onEmergencyContactNameChanged(value: String) { _emergencyContactName.value = value }
    fun onEmergencyContactPhoneChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(10)
        _emergencyContactPhone.value = digitsOnly
    }
    fun onAdditionalNotesChanged(value: String) { _additionalNotes.value = value }

    fun cancelEditing() {
        val initial = initialProfile
        if (initial != null) {
            populateFields(initial)
        }
        _validationError.value = null
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val uid = authRepository.currentUser?.uid?.ifBlank { null } ?: "local_user"
        val name = _fullName.value.trim()
        if (name.isBlank()) {
            _validationError.value = "Full name is required to save patient profile."
            return
        }

        viewModelScope.launch(dispatcherProvider.main) {
            _isSaving.value = true
            val profile = PatientProfile(
                accountUid = uid,
                fullName = name,
                dateOfBirth = _dateOfBirth.value.trim().ifBlank { null },
                gender = _gender.value.ifBlank { null },
                city = _city.value.trim().ifBlank { null },
                state = _state.value.trim().ifBlank { null },
                preferredLanguage = _preferredLanguage.value,
                photoUri = _photoUri.value,
                bloodGroup = _bloodGroup.value.trim().ifBlank { null },
                allergies = _allergies.value.trim().ifBlank { null },
                healthConditions = _healthConditions.value.trim().ifBlank { null },
                emergencyContactName = _emergencyContactName.value.trim().ifBlank { null },
                emergencyContactPhone = _emergencyContactPhone.value.trim().ifBlank { null },
                additionalNotes = _additionalNotes.value.trim().ifBlank { null }
            )

            profileRepository.saveProfile(profile)
            userPreferencesRepository?.setPatientName(name)
            initialProfile = profile
            _isSaving.value = false
            _saveSuccess.value = true
            onSuccess()
        }
    }
}
