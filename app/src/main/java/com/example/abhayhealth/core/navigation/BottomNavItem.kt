package com.example.abhayhealth.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.abhayhealth.R

/**
 * Bottom navigation bar tabs for the Patient experience.
 */
sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = NavRoutes.MainTab.HOME,
        titleRes = R.string.nav_home,
        icon = Icons.Default.Home
    )

    data object Assistant : BottomNavItem(
        route = NavRoutes.MainTab.AI_ASSISTANT,
        titleRes = R.string.nav_ai_assistant,
        icon = Icons.Default.AutoAwesome
    )

    data object Records : BottomNavItem(
        route = NavRoutes.MainTab.RECORDS,
        titleRes = R.string.nav_records,
        icon = Icons.AutoMirrored.Filled.ReceiptLong
    )

    data object Medicines : BottomNavItem(
        route = NavRoutes.MainTab.MEDICINES,
        titleRes = R.string.nav_medicines,
        icon = Icons.Default.MedicalServices
    )

    data object Me : BottomNavItem(
        route = NavRoutes.MainTab.ME,
        titleRes = R.string.nav_me,
        icon = Icons.Default.Person
    )

    companion object {
        val items = listOf(Home, Assistant, Records, Medicines, Me)
    }
}
