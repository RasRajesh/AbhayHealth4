package com.example.abhayhealth.domain.usecase

import com.example.abhayhealth.domain.repository.UserPreferencesRepository

class CompleteOnboardingUseCase(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(patientName: String = "") {
        if (patientName.isNotBlank()) {
            repository.setPatientName(patientName)
        }
        repository.setOnboardingCompleted(true)
    }
}
