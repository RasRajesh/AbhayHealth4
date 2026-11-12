package com.example.abhayhealth.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDeepTeal,
    onPrimary = OnPrimaryTeal,
    primaryContainer = PrimaryContainerMint,
    onPrimaryContainer = OnPrimaryContainerMint,
    secondary = SecondaryTeal,
    onSecondary = OnSecondaryTeal,
    secondaryContainer = SecondaryContainerSoft,
    onSecondaryContainer = OnSecondaryContainerSoft,
    tertiary = TertiaryCoral,
    onTertiary = OnTertiaryCoral,
    tertiaryContainer = TertiaryContainerCoral,
    onTertiaryContainer = OnTertiaryContainerCoral,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed,
    background = WarmWhiteBackground,
    onBackground = OnWarmWhiteBackground,
    surface = SurfaceWhite,
    onSurface = OnSurfaceWhite,
    surfaceVariant = SurfaceVariantSoft,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineTeal,
    outlineVariant = OutlineVariantTeal
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealDark,
    onPrimary = OnPrimaryTealDark,
    primaryContainer = PrimaryContainerTealDark,
    onPrimaryContainer = OnPrimaryContainerTealDark,
    secondary = SecondaryTealDark,
    onSecondary = OnSecondaryTealDark,
    secondaryContainer = SecondaryContainerTealDark,
    onSecondaryContainer = OnSecondaryContainerTealDark,
    error = ErrorRedDark,
    onError = OnErrorRedDark,
    errorContainer = ErrorContainerRedDark,
    onErrorContainer = OnErrorContainerRedDark,
    background = BackgroundSlateDark,
    onBackground = OnBackgroundSlateDark,
    surface = SurfaceSlateDark,
    onSurface = OnSurfaceSlateDark,
    surfaceVariant = SurfaceVariantSlateDark,
    onSurfaceVariant = OnSurfaceVariantSlateDark,
    outline = OutlineSlateDark,
    outlineVariant = OutlineVariantSlateDark
)

@Composable
fun AbhayHealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep default false to preserve consistent, luxurious AbhayHealth palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
