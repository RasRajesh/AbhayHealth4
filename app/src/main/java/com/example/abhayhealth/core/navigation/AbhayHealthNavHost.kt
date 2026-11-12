package com.example.abhayhealth.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.feature.auth.AuthViewModel
import com.example.abhayhealth.feature.auth.EnterPhoneScreen
import com.example.abhayhealth.feature.auth.VerifyOtpScreen
import com.example.abhayhealth.feature.auth.WelcomeScreen
import com.example.abhayhealth.feature.home.HomeViewModel
import com.example.abhayhealth.feature.main.MainScreen
import com.example.abhayhealth.feature.me.MeViewModel
import com.example.abhayhealth.feature.medicines.MedicinesViewModel
import com.example.abhayhealth.feature.onboarding.OnboardingScreen
import com.example.abhayhealth.feature.onboarding.OnboardingViewModel
import com.example.abhayhealth.feature.profile.ProfileEditScreen
import com.example.abhayhealth.feature.profile.ProfileSetupScreen
import com.example.abhayhealth.feature.profile.ProfileViewModel
import com.example.abhayhealth.feature.records.ExtractionReviewScreen
import com.example.abhayhealth.feature.records.ExtractionReviewViewModel
import com.example.abhayhealth.feature.records.RecordsViewModel
import com.example.abhayhealth.feature.splash.SplashScreen
import com.example.abhayhealth.feature.splash.SplashViewModel
import com.example.abhayhealth.feature.trends.TrendsViewModel

import com.example.abhayhealth.feature.assistant.AiAssistantViewModel

@Composable
fun AbhayHealthNavHost(
    splashViewModel: SplashViewModel,
    onboardingViewModel: OnboardingViewModel,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    extractionReviewViewModel: ExtractionReviewViewModel,
    homeViewModel: HomeViewModel,
    recordsViewModel: RecordsViewModel,
    trendsViewModel: TrendsViewModel,
    medicinesViewModel: MedicinesViewModel,
    meViewModel: MeViewModel,
    modifier: Modifier = Modifier,
    aiAssistantViewModel: AiAssistantViewModel? = null,
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.authState.collectAsState()

    // Observe global authentication state for seamless navigation transitions
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.CodeSent -> {
                if (navController.currentDestination?.route != NavRoutes.VERIFY_OTP) {
                    navController.navigate(NavRoutes.VERIFY_OTP)
                }
            }
            is AuthState.Authenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == NavRoutes.WELCOME || currentRoute == NavRoutes.VERIFY_OTP || currentRoute == NavRoutes.ENTER_PHONE || currentRoute == NavRoutes.ONBOARDING) {
                    profileViewModel.loadProfileForCurrentUser()
                    navController.navigate(NavRoutes.PROFILE_SETUP) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        popUpTo(NavRoutes.WELCOME) { inclusive = true }
                        popUpTo(NavRoutes.ENTER_PHONE) { inclusive = true }
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == NavRoutes.MAIN || currentRoute == NavRoutes.PROFILE_EDIT || currentRoute?.startsWith("extraction_review") == true) {
                    navController.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.MAIN) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
        exitTransition = { fadeOut() + slideOutHorizontally { -it / 4 } },
        popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
        popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } },
        modifier = modifier
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateNext = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo(NavRoutes.SPLASH) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(NavRoutes.ONBOARDING) {
            EnterPhoneScreen(
                viewModel = authViewModel,
                onboardingViewModel = onboardingViewModel,
                onBackClick = null
            )
        }

        composable(NavRoutes.WELCOME) {
            EnterPhoneScreen(
                viewModel = authViewModel,
                onboardingViewModel = onboardingViewModel,
                onBackClick = null
            )
        }

        composable(NavRoutes.ENTER_PHONE) {
            EnterPhoneScreen(
                viewModel = authViewModel,
                onboardingViewModel = onboardingViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.VERIFY_OTP) {
            VerifyOtpScreen(
                viewModel = authViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.PROFILE_SETUP) {
            ProfileSetupScreen(
                viewModel = profileViewModel,
                onSetupFinished = {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.PROFILE_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.PROFILE_EDIT) {
            val userPhone = (authState as? AuthState.Authenticated)?.user?.phoneNumber
            ProfileEditScreen(
                viewModel = profileViewModel,
                userPhone = userPhone,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoutes.EXTRACTION_REVIEW,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
            LaunchedEffect(recordId) {
                if (recordId != -1L) {
                    extractionReviewViewModel.loadRecordAndInfo(recordId)
                }
            }
            ExtractionReviewScreen(
                viewModel = extractionReviewViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.MAIN) {
            MainScreen(
                homeViewModel = homeViewModel,
                recordsViewModel = recordsViewModel,
                trendsViewModel = trendsViewModel,
                medicinesViewModel = medicinesViewModel,
                meViewModel = meViewModel,
                aiAssistantViewModel = aiAssistantViewModel,
                onEditProfileClick = {
                    profileViewModel.loadProfileForCurrentUser()
                    navController.navigate(NavRoutes.PROFILE_EDIT)
                },
                onRecordReviewClick = { recordId ->
                    navController.navigate(NavRoutes.extractionReview(recordId))
                },
                onSignOutClick = {
                    authViewModel.signOut()
                }
            )
        }
    }
}
