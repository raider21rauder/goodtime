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

import android.annotation.SuppressLint
import android.os.SystemClock
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.BuildConfig
import com.apps.adrcotfas.goodtime.bl.FinishActionType
import com.apps.adrcotfas.goodtime.bl.getLabelData
import com.apps.adrcotfas.goodtime.common.installIsOlderThan10Days
import com.apps.adrcotfas.goodtime.common.isPortrait
import com.apps.adrcotfas.goodtime.common.screenWidth
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialConfig
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControl
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControlButton
import com.apps.adrcotfas.goodtime.main.dialcontrol.rememberCustomDialControlState
import com.apps.adrcotfas.goodtime.main.dialcontrol.updateEnabledOptions
import com.apps.adrcotfas.goodtime.main.finishedsession.FinishedSessionSheet
import com.apps.adrcotfas.goodtime.settings.permissions.getPermissionsState
import com.apps.adrcotfas.goodtime.settings.timerstyle.InitTimerStyle
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.SelectLabelDialog
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Edit
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    navController: NavController,
    onSurfaceClick: () -> Unit,
    hideBottomBar: Boolean,
    viewModel: TimerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(TimerMainUiState())
    if (uiState.isLoading) return
    val context = LocalContext.current

    InitTimerStyle(viewModel)

    LifecycleResumeEffect(Unit) {
        viewModel.refreshStartOfToday()
        onPauseOrDispose {
            // do nothing
        }
    }

    val timerUiState by viewModel.timerUiState.collectAsStateWithLifecycle(TimerUiState())

    val timerStyle = uiState.timerStyle
    val label = timerUiState.label

    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current

    val dialControlState = rememberCustomDialControlState(
        config = DialConfig(size = configuration.screenWidth),
        onTop = viewModel::addOneMinute,
        onRight = viewModel::skip,
        onBottom = viewModel::resetTimer,
    )

    dialControlState.updateEnabledOptions(timerUiState)
    val gestureModifier = dialControlState.let {
        Modifier
            .pointerInput(it) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    it.onDown()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

    val yOffset = remember { Animatable(0f) }
    ScreensaverMode(
        screensaverMode = uiState.screensaverMode,
        isActive = timerUiState.isActive,
        screenWidth = configuration.screenWidth,
        yOffset = yOffset,
    )

    val backgroundColor by animateColorAsState(
        if (uiState.darkThemePreference.isDarkTheme(isSystemInDarkTheme()) &&
            uiState.trueBlackMode &&
            timerUiState.isActive &&
            hideBottomBar
        ) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "main background color",
    )

    val permissionsState = getPermissionsState()
    val settingsBadgeItemCount = listOf(
        permissionsState.shouldAskForNotificationPermission,
        permissionsState.shouldAskForBatteryOptimizationRemoval,
    ).count { state -> state }

    val interactionSource = remember { MutableInteractionSource() }

    var showNavigationSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectLabelDialog by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        timerUiState.isReady,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        onSurfaceClick()
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
                        onStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.startTimer()
                        },
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleTimer()
                        },
                        onLongClick = { navController.navigate(SettingsDest) },
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
                        hide = hideBottomBar,
                        onShowSheet = { showNavigationSheet = true },
                        onLabelClick = { showSelectLabelDialog = true },
                        labelData = label.getLabelData(),
                        sessionCountToday = uiState.sessionCountToday,
                        badgeItemCount = settingsBadgeItemCount,
                        navController = navController,
                    )
                }
            }
        }
    }

    if (showNavigationSheet) {
        MainNavigationSheet(
            onHideSheet = { showNavigationSheet = false },
            navController = navController,
            settingsBadgeItemCount = settingsBadgeItemCount,
            showPro = BuildConfig.IS_FDROID || !uiState.isPro,
        )
    }

    var showFinishedSessionSheet by remember(timerUiState.isFinished) {
        mutableStateOf(timerUiState.isFinished)
    }

    val timeSinceFinished = if (timerUiState.endTime == 0L) {
        0
    } else {
        SystemClock.elapsedRealtime() - timerUiState.endTime
    }
    if (showFinishedSessionSheet && timeSinceFinished.milliseconds < 30.minutes) {
        FinishedSessionSheet(
            timerUiState = timerUiState,
            onHideSheet = { showFinishedSessionSheet = false },
            onNext = {
                viewModel.next()
                // ask for in app review if the user just started a break session
                if (context.installIsOlderThan10Days() && !timerUiState.isBreak) {
                    viewModel.setShouldAskForReview()
                }
            },
            onReset = { updateWorkTime ->
                if (updateWorkTime) {
                    viewModel.resetTimer(updateWorkTime = true)
                } else {
                    viewModel.resetTimer(false, actionType = FinishActionType.MANUAL_DO_NOTHING)
                }
            },
            onUpdateNotes = viewModel::updateNotesForLastCompletedSession,
        )
    }

    if (showSelectLabelDialog) {
        SelectLabelDialog(
            title = stringResource(R.string.labels_select_active_label),
            singleSelection = true,
            labels = uiState.labels,
            initialSelectedLabels = listOf(timerUiState.label.label.name),
            onDismiss = { showSelectLabelDialog = false },
            onConfirm = { selectedLabels ->
                if (selectedLabels.isNotEmpty()) {
                    val first = selectedLabels.first()
                    if (first != label.label.name) {
                        viewModel.setActiveLabel(first)
                    }
                }
                showSelectLabelDialog = false
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                ) {
                    TextButton(onClick = {
                        navController.navigate(LabelsDest)
                        showSelectLabelDialog = false
                    }) { Text(stringResource(R.string.labels_edit_labels)) }
                    FilledTonalButton(onClick = {
                        navController.navigate(AddEditLabelDest(name = timerUiState.label.getLabelName()))
                        showSelectLabelDialog = false
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = EvaIcons.Outline.Edit,
                                contentDescription = null,
                            )
                            Text(stringResource(R.string.labels_edit_active_label))
                        }
                    }
                }
            },
        )
    }
}
