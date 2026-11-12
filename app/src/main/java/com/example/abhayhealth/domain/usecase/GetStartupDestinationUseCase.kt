package com.example.abhayhealth.domain.usecase

import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.domain.repository.AuthRepository
import com.example.abhayhealth.domain.repository.PatientProfileRepository
import com.example.abhayhealth.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetStartupDestinationUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: PatientProfileRepository
) {
    operator fun invoke(): Flow<StartupDestination> {
        return combine(
            userPreferencesRepository.isOnboardingCompleted,
            authRepository.authState
        ) { onboardingCompleted, authState ->
            if (!onboardingCompleted) {
                StartupDestination.Onboarding
            } else if (authState is AuthState.Authenticated) {
                val uid = authState.user.uid
                val hasProfile = profileRepository.hasProfile(uid)
                if (hasProfile) StartupDestination.Main else StartupDestination.ProfileSetup
            } else if (authRepository.isAuthenticated) {
                val uid = authRepository.currentUser?.uid
                if (uid != null && profileRepository.hasProfile(uid)) {
                    StartupDestination.Main
                } else {
                    StartupDestination.ProfileSetup
                }
            } else {
                StartupDestination.Onboarding
            }
        }
    }
}

sealed interface StartupDestination {
    data object Onboarding : StartupDestination
    data object Welcome : StartupDestination
    data object ProfileSetup : StartupDestination
    data object Main : StartupDestination
}
