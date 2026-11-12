package com.example.abhayhealth.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhayhealth.core.navigation.NavRoutes
import com.example.abhayhealth.domain.usecase.GetStartupDestinationUseCase
import com.example.abhayhealth.domain.usecase.StartupDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val getStartupDestinationUseCase: GetStartupDestinationUseCase
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        determineDestination()
    }

    private fun determineDestination() {
        viewModelScope.launch {
            // Branding display time
            delay(1000)
            getStartupDestinationUseCase().collect { destination ->
                _startDestination.value = when (destination) {
                    StartupDestination.Onboarding -> NavRoutes.ONBOARDING
                    StartupDestination.Welcome -> NavRoutes.WELCOME
                    StartupDestination.ProfileSetup -> NavRoutes.PROFILE_SETUP
                    StartupDestination.Main -> NavRoutes.MAIN
                }
            }
        }
    }
}
