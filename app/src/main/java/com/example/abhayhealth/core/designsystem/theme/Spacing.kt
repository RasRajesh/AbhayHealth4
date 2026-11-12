package com.example.abhayhealth.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for accessible and consistent UI layout.
 */
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val touchTargetMinimum: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialThemeSpacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
