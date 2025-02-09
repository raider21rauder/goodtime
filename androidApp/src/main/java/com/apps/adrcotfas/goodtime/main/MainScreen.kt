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
package com.apps.adrcotfas.goodtime.main

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.bl.isWork
import com.apps.adrcotfas.goodtime.common.isPortrait
import com.apps.adrcotfas.goodtime.common.screenWidth
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialConfig
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControl
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControlButton
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialRegion
import com.apps.adrcotfas.goodtime.main.dialcontrol.rememberDialControlState
import com.apps.adrcotfas.goodtime.main.finishedsession.FinishedSessionSheet
import com.apps.adrcotfas.goodtime.settings.timerstyle.InitTimerStyle
import com.apps.adrcotfas.goodtime.ui.localColorsPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private var fullScreenJob: Job? = null

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
) {
    val activity = LocalActivity.current as ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle(MainUiState())
    if (uiState.isLoading) return
    InitTimerStyle(viewModel)

    val timerUiState by viewModel.timerUiState.collectAsStateWithLifecycle(TimerUiState())

    val timerStyle = uiState.timerStyle
    val label = timerUiState.label
    val labelColor = MaterialTheme.localColorsPalette.colors[label.label.colorIndex.toInt()]

    val configuration = LocalConfiguration.current
    val dialControlState = rememberDialControlState(
        options = DialRegion.entries,
        config = DialConfig(size = configuration.screenWidth),
        onSelected = {
            when (it) {
                DialRegion.TOP -> {
                    viewModel.addOneMinute()
                }

                DialRegion.RIGHT -> {
                    viewModel.skip()
                }

                DialRegion.BOTTOM -> {
                    viewModel.resetTimer()
                }

                else -> {
                }
            }
        },
    )

    val yOffset = remember { Animatable(0f) }
    ScreensaverMode(
        screensaverMode = uiState.screensaverMode,
        isActive = timerUiState.isActive,
        screenWidth = configuration.screenWidth,
        yOffset = yOffset,
    )

    val thereIsNoBreakBudget =
        timerUiState.breakBudgetMinutes == 0L
    val isCountUpWithoutBreaks = !label.profile.isCountdown && !label.profile.isBreakEnabled

    val disabledOptions = listOfNotNull(
        DialRegion.LEFT,
        if (!label.profile.isCountdown) {
            DialRegion.TOP
        } else {
            null
        },
        if ((!label.profile.isCountdown && thereIsNoBreakBudget && timerUiState.timerType.isWork) ||
            isCountUpWithoutBreaks
        ) {
            DialRegion.RIGHT
        } else {
            null
        },
    )

    dialControlState.updateEnabledOptions(disabledOptions)
    val gestureModifier = dialControlState.let {
        Modifier
            .pointerInput(it) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    it.onDown()
                    var change =
                        awaitTouchSlopOrCancellation(pointerId = down.id) { change, _ ->
                            change.consume()
                        }
                    while (change != null && change.pressed) {
                        change = awaitDragOrCancellation(change.id)?.also { inputChange ->
                            if (inputChange.pressed && timerUiState.isActive) {
                                dialControlState.onDrag(dragAmount = inputChange.positionChange())
                            }
                        }
                    }
                    it.onRelease()
                }
            }
    }

    val backgroundColor by animateColorAsState(
        if (uiState.isDarkTheme(isSystemInDarkTheme()) &&
            uiState.trueBlackMode &&
            timerUiState.isActive
        ) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "main background color",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isActive by viewModel.timerUiState.map { it.timerState.isActive }
        .collectAsStateWithLifecycle(false)

    val fullscreenMode = uiState.isMainScreen && uiState.fullscreenMode && isActive
    var hideBottomBarWhenActive by remember(fullscreenMode) {
        mutableStateOf(fullscreenMode)
    }

    var showNavigationSheet by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        timerUiState.isReady,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LaunchedEffect(fullscreenMode) {
            fullscreenMode.let {
                toggleFullscreen(activity, it)
                if (!it) fullScreenJob?.cancel()
            }
        }
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Surface(
                modifier = Modifier
                    .consumeWindowInsets(innerPadding)
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        if (fullscreenMode) {
                            fullScreenJob?.cancel()
                            fullScreenJob = coroutineScope.launch {
                                toggleFullscreen(activity, false)
                                hideBottomBarWhenActive = false
                                executeDelayed(3000) {
                                    toggleFullscreen(activity, true)
                                    hideBottomBarWhenActive = true
                                }
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .background(backgroundColor)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val modifier = Modifier.offset {
                        if (configuration.isPortrait) {
                            IntOffset(
                                0,
                                yOffset.value.roundToInt(),
                            )
                        } else {
                            IntOffset(yOffset.value.roundToInt(), 0)
                        }
                    }

                    val alphaModifier = Modifier.graphicsLayer {
                        alpha = if (dialControlState.isDragging) 0.38f else 1f
                    }
                    MainTimerView(
                        modifier = alphaModifier.then(modifier),
                        state = dialControlState,
                        gestureModifier = gestureModifier,
                        timerUiState = timerUiState,
                        timerStyle = timerStyle,
                        domainLabel = label,
                        onStart = viewModel::startTimer,
                        onToggle = viewModel::toggleTimer,
                    )
                    DialControl(
                        modifier = modifier,
                        state = dialControlState,
                        dialContent = { region ->
                            DialControlButton(
                                disabled = dialControlState.isDisabled(region),
                                selected = region == dialControlState.selectedOption,
                                region = region,
                            )
                        },
                    )
                    BottomAppBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        hide = hideBottomBarWhenActive,
                        onShowSheet = { showNavigationSheet = true },
                        labelColor = labelColor,
                        navController = navController,
                    )
                }
            }
        }
    }

    if (showNavigationSheet) {
        BottomNavigationSheet(
            onHideSheet = { showNavigationSheet = false },
            navController = navController,
        )
    }

    var showFinishedSessionSheet by rememberSaveable(timerUiState.isFinished) {
        mutableStateOf(timerUiState.isFinished)
    }
    if (showFinishedSessionSheet) {
        FinishedSessionSheet(
            timerUiState = timerUiState,
            onHideSheet = { showFinishedSessionSheet = false },
            onNext = viewModel::next,
            onReset = viewModel::resetTimer,
            onUpdateNotes = viewModel::updateNotesForLastCompletedSession,
        )
    }
}

private fun toggleFullscreen(activity: ComponentActivity, enabled: Boolean) {
    val windowInsetsController =
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)

    if (enabled) {
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

private suspend fun executeDelayed(delay: Long, block: () -> Unit) {
    coroutineScope {
        delay(delay)
        block()
    }
}
