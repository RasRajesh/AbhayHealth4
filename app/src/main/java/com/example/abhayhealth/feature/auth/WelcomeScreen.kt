package com.example.abhayhealth.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Retained for route compatibility; delegates directly to the unified Get Started & Mobile Number screen.
 */
@Composable
fun WelcomeScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Automatically trigger continuation to unified phone login if ever navigated
    onContinueClick()
}
