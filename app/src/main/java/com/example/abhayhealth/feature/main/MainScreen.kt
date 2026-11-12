package com.example.abhayhealth.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.abhayhealth.core.navigation.BottomNavItem
import com.example.abhayhealth.core.navigation.NavRoutes
import com.example.abhayhealth.feature.home.HomeScreen
import com.example.abhayhealth.feature.home.HomeViewModel
import com.example.abhayhealth.feature.me.MeScreen
import com.example.abhayhealth.feature.me.MeViewModel
import com.example.abhayhealth.feature.medicines.MedicinesScreen
import com.example.abhayhealth.feature.medicines.MedicinesViewModel
import com.example.abhayhealth.feature.assistant.AiAssistantScreen
import com.example.abhayhealth.feature.assistant.AiAssistantViewModel
import com.example.abhayhealth.feature.records.RecordsScreen
import com.example.abhayhealth.feature.records.RecordsViewModel
import com.example.abhayhealth.feature.trends.TrendsScreen
import com.example.abhayhealth.feature.trends.TrendsViewModel

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    recordsViewModel: RecordsViewModel,
    trendsViewModel: TrendsViewModel,
    medicinesViewModel: MedicinesViewModel,
    meViewModel: MeViewModel,
    onEditProfileClick: () -> Unit,
    onRecordReviewClick: (Long) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    aiAssistantViewModel: AiAssistantViewModel? = null,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                BottomNavItem.items.forEach { item ->
                    val title = stringResource(id = item.titleRes)
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = title
                            )
                        },
                        label = { Text(text = title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.MainTab.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.MainTab.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onAddRecordClick = {
                        navController.navigate(NavRoutes.MainTab.RECORDS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(NavRoutes.MainTab.AI_ASSISTANT) {
                if (aiAssistantViewModel != null) {
                    AiAssistantScreen(viewModel = aiAssistantViewModel)
                }
            }
            composable(NavRoutes.MainTab.RECORDS) {
                RecordsScreen(
                    viewModel = recordsViewModel,
                    onReviewClick = onRecordReviewClick
                )
            }
            composable(NavRoutes.MainTab.TRENDS) {
                TrendsScreen(viewModel = trendsViewModel)
            }
            composable(NavRoutes.MainTab.MEDICINES) {
                MedicinesScreen(viewModel = medicinesViewModel)
            }
            composable(NavRoutes.MainTab.ME) {
                MeScreen(
                    viewModel = meViewModel,
                    onEditProfileClick = onEditProfileClick,
                    onSignOutClick = onSignOutClick,
                    onNavigateToRecords = {
                        navController.navigate(NavRoutes.MainTab.RECORDS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMedicines = {
                        navController.navigate(NavRoutes.MainTab.MEDICINES) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
