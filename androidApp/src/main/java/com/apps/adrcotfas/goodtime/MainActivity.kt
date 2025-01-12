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

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.bl.isFinished
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.di.injectLogger
import com.apps.adrcotfas.goodtime.main.Destination
import com.apps.adrcotfas.goodtime.main.MainViewModel
import com.apps.adrcotfas.goodtime.main.bottomNavigationItems
import com.apps.adrcotfas.goodtime.onboarding.OnboardingScreen
import com.apps.adrcotfas.goodtime.onboarding.OnboardingViewModel
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val log: Logger by injectLogger("MainActivity")

    private val notificationManager: NotificationArchManager by inject()

    private var fullScreenJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.d { "onCreate" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }

        setContent {
            val onboardingViewModel = koinViewModel<OnboardingViewModel>()
            val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()

            // TODO: add loading/splash screen
            if (onboardingState.loading) {
                return@setContent
            }

            val viewModel =
                koinViewModel<MainViewModel>(viewModelStoreOwner = LocalContext.current as ComponentActivity)
            val coroutineScope = rememberCoroutineScope()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val workSessionIsInProgress by viewModel.timerUiState.map { it.workSessionIsInProgress() }
                .collectAsStateWithLifecycle(false)
            val isActive by viewModel.timerUiState.map { it.timerState.isActive }
                .collectAsStateWithLifecycle(false)
            val isFinished by viewModel.timerUiState.map { it.timerState.isFinished }
                .collectAsStateWithLifecycle(false)

            val fullscreenMode = uiState.isMainScreen && uiState.fullscreenMode && isActive
            var hideBottomBarWhenActive by remember(fullscreenMode) {
                mutableStateOf(fullscreenMode)
            }

            val isDarkTheme = uiState.isDarkTheme(isSystemInDarkTheme())

            toggleKeepScreenOn(isActive)
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                if (uiState.dndDuringWork) {
                    notificationManager.toggleDndMode(workSessionIsInProgress)
                } else {
                    notificationManager.toggleDndMode(false)
                }
            }

            var currentDestination by rememberSaveable { mutableStateOf(Destination.Main.route) }

            val isMainDestination =
                bottomNavigationItems.find { it.route == currentDestination } != null
            viewModel.setIsMainScreen(currentDestination == Destination.Main.route)
            val showNavigation = isMainDestination.xor(hideBottomBarWhenActive)

            LaunchedEffect(isFinished) {
                if (isFinished && currentDestination != Destination.Main.route) {
                    currentDestination = Destination.Main.route
                }
            }

            LaunchedEffect(fullscreenMode) {
                fullscreenMode.let {
                    toggleFullscreen(it)
                    if (!it) fullScreenJob?.cancel()
                }
            }

            val considerDarkTheme = if (!onboardingState.finished) {
                false
            } else {
                isDarkTheme
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
                val interactionSource = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            if (fullscreenMode) {
                                fullScreenJob?.cancel()
                                fullScreenJob = coroutineScope.launch {
                                    toggleFullscreen(false)
                                    hideBottomBarWhenActive = false
                                    executeDelayed(3000) {
                                        toggleFullscreen(true)
                                        hideBottomBarWhenActive = true
                                    }
                                }
                            }
                        },
                ) {
                    if (!onboardingState.finished) {
                        OnboardingScreen()
                    } else {
                        NavigationScaffold(
                            currentDestination = currentDestination,
                            showNavigation = showNavigation,
                            onNavigationChange = { newDestination ->
                                if (newDestination != currentDestination) {
                                    currentDestination = newDestination
                                }
                            },
                        )
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

    private fun toggleFullscreen(enabled: Boolean) {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)

        if (enabled) {
            windowInsetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private suspend fun executeDelayed(delay: Long, block: () -> Unit) {
    coroutineScope {
        delay(delay)
        block()
    }
}

private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
