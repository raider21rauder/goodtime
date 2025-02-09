/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.di.injectLogger
import com.apps.adrcotfas.goodtime.labels.addedit.AddEditLabelScreen
import com.apps.adrcotfas.goodtime.labels.archived.ArchivedLabelsScreen
import com.apps.adrcotfas.goodtime.labels.main.LabelsScreen
import com.apps.adrcotfas.goodtime.main.AboutDest
import com.apps.adrcotfas.goodtime.main.AddEditLabelDest
import com.apps.adrcotfas.goodtime.main.ArchivedLabelsDest
import com.apps.adrcotfas.goodtime.main.BackupDest
import com.apps.adrcotfas.goodtime.main.GeneralSettingsDest
import com.apps.adrcotfas.goodtime.main.LabelsDest
import com.apps.adrcotfas.goodtime.main.LicensesDest
import com.apps.adrcotfas.goodtime.main.MainDest
import com.apps.adrcotfas.goodtime.main.MainScreen
import com.apps.adrcotfas.goodtime.main.MainViewModel
import com.apps.adrcotfas.goodtime.main.NotificationSettingsDest
import com.apps.adrcotfas.goodtime.main.OnboardingDest
import com.apps.adrcotfas.goodtime.main.SettingsDest
import com.apps.adrcotfas.goodtime.main.StatsDest
import com.apps.adrcotfas.goodtime.main.TimerStyleDest
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.main.route
import com.apps.adrcotfas.goodtime.onboarding.OnboardingScreen
import com.apps.adrcotfas.goodtime.onboarding.OnboardingViewModel
import com.apps.adrcotfas.goodtime.settings.SettingsScreen
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.settings.about.AboutScreen
import com.apps.adrcotfas.goodtime.settings.about.LicensesScreen
import com.apps.adrcotfas.goodtime.settings.backup.BackupScreen
import com.apps.adrcotfas.goodtime.settings.general.GeneralSettingsScreen
import com.apps.adrcotfas.goodtime.settings.notifications.NotificationsScreen
import com.apps.adrcotfas.goodtime.settings.timerstyle.TimerStyleScreen
import com.apps.adrcotfas.goodtime.stats.StatisticsScreen
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val log: Logger by injectLogger("MainActivity")
    private val notificationManager: NotificationArchManager by inject()
    private val viewModel: MainViewModel by viewModel<MainViewModel>()
    private val onboardingViewModel: OnboardingViewModel by viewModel<OnboardingViewModel>()

    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.d { "onCreate" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()

            // TODO: add loading/splash screen
            if (onboardingState.loading) {
                return@setContent
            }

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDarkTheme = uiState.isDarkTheme(isSystemInDarkTheme())

            val timerUiState by viewModel.timerUiState.collectAsStateWithLifecycle(TimerUiState())
            val workSessionIsInProgress = timerUiState.workSessionIsInProgress()
            val isActive = timerUiState.isActive
            val isFinished = timerUiState.isFinished

            toggleKeepScreenOn(isActive)
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                if (uiState.dndDuringWork) {
                    notificationManager.toggleDndMode(workSessionIsInProgress)
                } else {
                    notificationManager.toggleDndMode(false)
                }
            }

            val considerDarkTheme = remember(onboardingState.finished) {
                if (!onboardingState.finished) {
                    false
                } else {
                    isDarkTheme
                }
            }
            val startDestination = remember(onboardingState.finished) {
                if (onboardingState.finished) {
                    MainDest
                } else {
                    OnboardingDest
                }
            }

            DisposableEffect(considerDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { considerDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { considerDarkTheme },
                )
                onDispose {}
            }

            ApplicationTheme(darkTheme = isDarkTheme, dynamicColor = uiState.dynamicColor) {
                val navController = rememberNavController()
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    val isMainScreen = destination.route == MainDest.route
                    viewModel.setIsMainScreen(isMainScreen)
                }

                LaunchedEffect(isFinished) {
                    val isMainScreen = navController.currentDestination?.route == MainDest.route
                    if (isFinished && !isMainScreen) navController.navigate(MainDest)
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    composable<OnboardingDest> {
                        OnboardingScreen()
                    }
                    composable<MainDest> { MainScreen(navController = navController) }
                    composable<LabelsDest> {
                        LabelsScreen(
                            onNavigateToLabel = navController::navigate,
                            onNavigateToArchivedLabels = {
                                navController.navigate(ArchivedLabelsDest)
                            },
                            onNavigateBack = navController::popBackStack,
                        )
                    }
                    composable<AddEditLabelDest> { backStackEntry ->
                        val addEditLabelDest = backStackEntry.toRoute<AddEditLabelDest>()
                        AddEditLabelScreen(
                            labelName = addEditLabelDest.name,
                            onNavigateBack = navController::popBackStack,
                        )
                    }
                    composable<ArchivedLabelsDest> {
                        ArchivedLabelsScreen(onNavigateBack = navController::popBackStack)
                    }
                    composable<StatsDest> { StatisticsScreen(onNavigateBack = navController::popBackStack) }
                    composable<SettingsDest> {
                        val backStackEntry =
                            remember { navController.getBackStackEntry(SettingsDest) }
                        val viewModel: SettingsViewModel =
                            koinViewModel(viewModelStoreOwner = backStackEntry)
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToGeneralSettings = {
                                navController.navigate(
                                    GeneralSettingsDest,
                                )
                            },
                            onNavigateToTimerStyle = { navController.navigate(TimerStyleDest) },
                            onNavigateToNotifications = {
                                navController.navigate(
                                    NotificationSettingsDest,
                                )
                            },
                            onNavigateBack = navController::popBackStack,
                        )
                    }
                    composable<GeneralSettingsDest> {
                        val backStackEntry =
                            remember { navController.getBackStackEntry(SettingsDest) }
                        val viewModel: SettingsViewModel =
                            koinViewModel(viewModelStoreOwner = backStackEntry)
                        GeneralSettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = navController::popBackStack,
                        )
                    }
                    composable<TimerStyleDest> {
                        val backStackEntry =
                            remember { navController.getBackStackEntry(SettingsDest) }
                        val viewModel: SettingsViewModel =
                            koinViewModel(viewModelStoreOwner = backStackEntry)
                        TimerStyleScreen(
                            viewModel = viewModel,
                            onNavigateBack = navController::popBackStack,
                        )
                    }
                    composable<NotificationSettingsDest> {
                        val backStackEntry =
                            remember { navController.getBackStackEntry(SettingsDest) }
                        val viewModel: SettingsViewModel =
                            koinViewModel(viewModelStoreOwner = backStackEntry)
                        NotificationsScreen(
                            viewModel = viewModel,
                            onNavigateBack = navController::popBackStack,
                        )
                    }

                    composable<BackupDest> {
                        BackupScreen(onNavigateBack = navController::popBackStack)
                    }
                    composable<AboutDest> {
                        AboutScreen(
                            onNavigateToLicenses = {
                                navController.navigate(
                                    LicensesDest,
                                )
                            },
                            onNavigateBack = navController::popBackStack,
                        )
                    }
                    composable<LicensesDest> {
                        LicensesScreen(onNavigateBack = navController::popBackStack)
                    }
                }
            }
        }
    }

    private fun toggleKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
