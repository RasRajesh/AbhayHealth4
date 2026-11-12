package com.example.abhayhealth

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.abhayhealth.core.designsystem.theme.AbhayHealthTheme
import com.example.abhayhealth.core.navigation.AbhayHealthNavHost
import com.example.abhayhealth.core.storage.datastore.UserPreferences
import java.util.Locale
import com.example.abhayhealth.data.repository.FirebaseAuthRepository
import com.example.abhayhealth.data.repository.PatientProfileRepositoryImpl
import com.example.abhayhealth.data.repository.UserPreferencesRepositoryImpl
import com.example.abhayhealth.domain.usecase.CompleteOnboardingUseCase
import com.example.abhayhealth.domain.usecase.GetStartupDestinationUseCase
import com.example.abhayhealth.feature.assistant.AiAssistantViewModel
import com.example.abhayhealth.feature.auth.AuthViewModel
import com.example.abhayhealth.feature.home.HomeViewModel
import com.example.abhayhealth.feature.me.MeViewModel
import com.example.abhayhealth.feature.medicines.MedicinesViewModel
import com.example.abhayhealth.feature.onboarding.OnboardingViewModel
import com.example.abhayhealth.feature.profile.ProfileViewModel
import com.example.abhayhealth.feature.records.ExtractionReviewViewModel
import com.example.abhayhealth.feature.records.MedicalRecordDatabase
import com.example.abhayhealth.feature.records.MedicalRecordRepository
import com.example.abhayhealth.feature.records.RecordsViewModel
import com.example.abhayhealth.feature.splash.SplashViewModel
import com.example.abhayhealth.feature.symptoms.SymptomLogRepository
import com.example.abhayhealth.feature.trends.TrendsViewModel
import com.example.abhayhealth.feature.vitals.VitalReadingRepository

@Composable
fun AbhayHealthApp(
    context: Context = LocalContext.current
) {
    // Composition root dependency graph initialization
    val userPreferences = remember(context) { UserPreferences(context.applicationContext) }
    val userPreferencesRepository = remember(userPreferences) { UserPreferencesRepositoryImpl(userPreferences) }
    val authRepository = remember { FirebaseAuthRepository(context.applicationContext) }
    val patientProfileRepository = remember(context) { PatientProfileRepositoryImpl(context.applicationContext) }

    val medicalRecordDatabase = remember(context) { MedicalRecordDatabase.getInstance(context.applicationContext) }
    val medicalRecordRepository = remember(medicalRecordDatabase) {
        MedicalRecordRepository(
            medicalRecordDatabase.medicalRecordDao(),
            medicalRecordDatabase.extractedInfoDao()
        )
    }
    val vitalReadingRepository = remember(medicalRecordDatabase) {
        VitalReadingRepository(medicalRecordDatabase.vitalReadingDao())
    }

    val getStartupDestinationUseCase = remember(userPreferencesRepository, authRepository, patientProfileRepository) {
        GetStartupDestinationUseCase(userPreferencesRepository, authRepository, patientProfileRepository)
    }
    val completeOnboardingUseCase = remember(userPreferencesRepository) {
        CompleteOnboardingUseCase(userPreferencesRepository)
    }

    val splashViewModel = remember(getStartupDestinationUseCase) { SplashViewModel(getStartupDestinationUseCase) }
    val onboardingViewModel = remember(completeOnboardingUseCase) { OnboardingViewModel(completeOnboardingUseCase) }
    val authViewModel = remember(authRepository) { AuthViewModel(authRepository) }
    val profileViewModel = remember(patientProfileRepository, authRepository, userPreferencesRepository) {
        ProfileViewModel(patientProfileRepository, authRepository, userPreferencesRepository)
    }
    val extractionReviewViewModel = remember(medicalRecordRepository, authRepository, medicalRecordDatabase, vitalReadingRepository) {
        ExtractionReviewViewModel(
            medicalRecordRepository = medicalRecordRepository,
            authRepository = authRepository,
            medicationDao = medicalRecordDatabase.medicationDao(),
            vitalReadingRepository = vitalReadingRepository
        )
    }
    val homeViewModel = remember(userPreferencesRepository, authRepository, medicalRecordRepository, medicalRecordDatabase, patientProfileRepository, vitalReadingRepository) {
        HomeViewModel(
            userPreferencesRepository = userPreferencesRepository,
            authRepository = authRepository,
            medicalRecordRepository = medicalRecordRepository,
            medicationDao = medicalRecordDatabase.medicationDao(),
            patientProfileRepository = patientProfileRepository,
            vitalReadingRepository = vitalReadingRepository
        )
    }
    val recordsViewModel = remember(medicalRecordRepository, authRepository, medicalRecordDatabase, vitalReadingRepository, patientProfileRepository) {
        RecordsViewModel(
            medicalRecordRepository = medicalRecordRepository,
            authRepository = authRepository,
            medicationDao = medicalRecordDatabase.medicationDao(),
            vitalReadingRepository = vitalReadingRepository,
            patientProfileRepository = patientProfileRepository
        )
    }
    val symptomLogRepository = remember(medicalRecordDatabase) {
        SymptomLogRepository(medicalRecordDatabase.symptomLogDao())
    }
    val trendsViewModel = remember(medicalRecordRepository, authRepository, symptomLogRepository, vitalReadingRepository) {
        TrendsViewModel(
            medicalRecordRepository = medicalRecordRepository,
            authRepository = authRepository,
            symptomLogRepository = symptomLogRepository,
            vitalReadingRepository = vitalReadingRepository
        )
    }
    val medicinesViewModel = remember(medicalRecordDatabase, authRepository, context) {
        MedicinesViewModel(medicalRecordDatabase.medicationDao(), authRepository, context.applicationContext)
    }
    val meViewModel = remember(userPreferencesRepository, authRepository, medicalRecordRepository, patientProfileRepository, medicalRecordDatabase, vitalReadingRepository) {
        MeViewModel(
            userPreferencesRepository = userPreferencesRepository,
            authRepository = authRepository,
            medicalRecordRepository = medicalRecordRepository,
            patientProfileRepository = patientProfileRepository,
            medicationDao = medicalRecordDatabase.medicationDao(),
            vitalReadingRepository = vitalReadingRepository
        )
    }
    val aiAssistantViewModel = remember(context, userPreferencesRepository, authRepository, medicalRecordRepository, medicalRecordDatabase, patientProfileRepository, vitalReadingRepository) {
        AiAssistantViewModel(
            context = context.applicationContext,
            userPreferencesRepository = userPreferencesRepository,
            authRepository = authRepository,
            medicalRecordRepository = medicalRecordRepository,
            medicationDao = medicalRecordDatabase.medicationDao(),
            patientProfileRepository = patientProfileRepository,
            vitalReadingRepository = vitalReadingRepository
        )
    }

    val appLanguage by userPreferencesRepository.appLanguage.collectAsState(initial = "English")
    val isTelugu = appLanguage.contains("Telugu", ignoreCase = true) || appLanguage.startsWith("te", ignoreCase = true)
    val targetLocale = remember(isTelugu) {
        if (isTelugu) Locale("te") else Locale("en")
    }

    val currentConfig = LocalConfiguration.current
    val localizedConfiguration = remember(currentConfig, targetLocale) {
        Configuration(currentConfig).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
    }

    val localizedContext = remember(context, targetLocale) {
        val config = Configuration(context.resources.configuration).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
        object : android.content.ContextWrapper(context) {
            private val configContext = context.createConfigurationContext(config)
            override fun getResources(): android.content.res.Resources = configContext.resources
            override fun getAssets(): android.content.res.AssetManager = configContext.assets
        }
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext
    ) {
        AbhayHealthTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AbhayHealthNavHost(
                    splashViewModel = splashViewModel,
                    onboardingViewModel = onboardingViewModel,
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel,
                    extractionReviewViewModel = extractionReviewViewModel,
                    homeViewModel = homeViewModel,
                    recordsViewModel = recordsViewModel,
                    trendsViewModel = trendsViewModel,
                    medicinesViewModel = medicinesViewModel,
                    meViewModel = meViewModel,
                    aiAssistantViewModel = aiAssistantViewModel
                )
            }
        }
    }
}
