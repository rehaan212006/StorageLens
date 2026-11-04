package com.storagelens.app.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.storagelens.app.StorageLensApplication
import com.storagelens.app.permissions.PermissionManager
import com.storagelens.app.ui.screens.explore.ExploreScreen
import com.storagelens.app.ui.screens.explore.ExploreViewModel
import com.storagelens.app.ui.screens.insights.InsightsScreen
import com.storagelens.app.ui.screens.insights.InsightsViewModel
import com.storagelens.app.ui.screens.largefiles.LargeFilesScreen
import com.storagelens.app.ui.screens.largefiles.LargeFilesViewModel
import com.storagelens.app.ui.screens.overview.OverviewScreen
import com.storagelens.app.ui.screens.overview.OverviewViewModel
import com.storagelens.app.ui.screens.settings.SettingsScreen
import com.storagelens.app.ui.screens.settings.SettingsViewModel

import com.storagelens.app.AppContainer

@Composable
fun StorageLensApp(container: AppContainer) {
    val context = LocalContext.current

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Overview.route

    var hasPermission by remember {
        mutableStateOf(PermissionManager.hasStoragePermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = PermissionManager.hasStoragePermissions(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            StorageLensBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Overview.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Overview.route) {
                val overviewViewModel: OverviewViewModel = viewModel(
                    factory = OverviewViewModel.provideFactory(
                        container.getDeviceStorageUseCase,
                        container.scanStorageUseCase
                    )
                )

                OverviewScreen(
                    viewModel = overviewViewModel,
                    hasStoragePermission = hasPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(PermissionManager.getRequiredStoragePermissions())
                    },
                    onNavigateToExplore = {
                        navController.navigate(Screen.Explore.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToLargeFiles = {
                        navController.navigate(Screen.LargeFiles.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.Insights.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Explore.route) {
                val exploreViewModel: ExploreViewModel = viewModel(
                    factory = ExploreViewModel.provideFactory(
                        container.storageRepository,
                        container.deleteFileUseCase
                    )
                )

                ExploreScreen(viewModel = exploreViewModel)
            }

            composable(Screen.LargeFiles.route) {
                val largeFilesViewModel: LargeFilesViewModel = viewModel(
                    factory = LargeFilesViewModel.provideFactory(
                        container.getLargeFilesUseCase,
                        container.deleteFileUseCase
                    )
                )

                LargeFilesScreen(
                    viewModel = largeFilesViewModel,
                    onNavigateToScan = {
                        navController.navigate(Screen.Overview.route)
                    }
                )
            }

            composable(Screen.Insights.route) {
                val insightsViewModel: InsightsViewModel = viewModel(
                    factory = InsightsViewModel.provideFactory(
                        container.getStorageInsightsUseCase,
                        container.findDuplicatesUseCase,
                        container.deleteFileUseCase
                    )
                )

                InsightsScreen(
                    viewModel = insightsViewModel,
                    onNavigateToScan = {
                        navController.navigate(Screen.Overview.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        container.preferencesRepository
                    )
                )

                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
