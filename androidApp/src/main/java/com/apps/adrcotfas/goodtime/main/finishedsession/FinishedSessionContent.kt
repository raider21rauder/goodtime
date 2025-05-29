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
package com.apps.adrcotfas.goodtime.main.finishedsession

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerManager.Companion.WIGGLE_ROOM_MILLIS
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.bl.isBreak
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.DragHandle
import com.apps.adrcotfas.goodtime.ui.common.TextBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedSessionSheet(
    viewModel: FinishedSessionViewModel = koinViewModel(),
    timerUiState: TimerUiState,
    onNext: (Boolean) -> Unit,
    onReset: (Boolean) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onHideSheet: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val finishedSessionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideFinishedSessionSheet = {
        coroutineScope.launch { finishedSessionSheetState.hide() }.invokeOnCompletion {
            if (!finishedSessionSheetState.isVisible) {
                onHideSheet()
            }
        }
    }

    val isBreak = rememberSaveable { timerUiState.timerType.isBreak }
    var updateWorkTime by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = {
            onUpdateNotes(notes)
            onReset(updateWorkTime)
            onHideSheet()
        },
        dragHandle = {
            DragHandle(
                buttonText =
                    if (isBreak) {
                        stringResource(R.string.main_start_focus)
                    } else {
                        stringResource(
                            R.string.main_start_break,
                        )
                    },
                onClose = {
                    onUpdateNotes(notes)
                    onReset(updateWorkTime)
                    hideFinishedSessionSheet()
                },
                onClick = {
                    onUpdateNotes(notes)
                    onNext(updateWorkTime)
                    hideFinishedSessionSheet()
                },
                isEnabled = true,
            )
        },
        sheetState = finishedSessionSheetState,
    ) {
        FinishedSessionContent(
            timerUiState = timerUiState,
            historyUiState = uiState,
            addIdleMinutes = updateWorkTime,
            onChangeAddIdleMinutes = { updateWorkTime = it },
            notes = notes,
            onNotesChanged = { notes = it },
        )
    }
}

// TODO: add a landscape mode too with the two cards side by side
@Composable
fun FinishedSessionContent(
    timerUiState: TimerUiState,
    historyUiState: HistoryUiState,
    addIdleMinutes: Boolean,
    onChangeAddIdleMinutes: (Boolean) -> Unit,
    notes: String,
    onNotesChanged: (String) -> Unit,
) {
    val timeProvider = koinInject<TimeProvider>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    var elapsedRealtime by remember(lifecycleState) { mutableLongStateOf(timeProvider.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1.minutes)
            elapsedRealtime = timeProvider.elapsedRealtime()
        }
    }
    FinishedSessionContent(
        timerUiState,
        historyUiState,
        elapsedRealtime,
        addIdleMinutes,
        onChangeAddIdleMinutes,
        notes,
        onNotesChanged,
    )
}

@Composable
private fun FinishedSessionContent(
    timerUiState: TimerUiState,
    historyUiState: HistoryUiState,
    elapsedRealtime: Long,
    addIdleMinutes: Boolean,
    onChangeAddIdleMinutes: (Boolean) -> Unit,
    notes: String,
    onNotesChanged: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isBreak = timerUiState.timerType.isBreak
        Text(
            text = if (isBreak) stringResource(R.string.main_break_complete) else stringResource(R.string.main_session_complete),
            style = MaterialTheme.typography.displaySmall,
        )
        CurrentSessionCard(
            timerUiState,
            elapsedRealtime,
            addIdleMinutes,
            onChangeAddIdleMinutes,
        )
        HistoryCard(historyUiState)

        Card {
            TextBox(
                modifier = Modifier.padding(16.dp),
                value = notes,
                onValueChange = onNotesChanged,
                enabled = historyUiState.isPro,
                placeholder = stringResource(R.string.stats_add_notes),
            )
        }
    }
}

@Composable
private fun CurrentSessionCard(
    timerUiState: TimerUiState,
    elapsedRealtime: Long,
    addIdleMinutes: Boolean,
    onAddIdleMinutesChanged: (Boolean) -> Unit,
) {
    val isBreak = timerUiState.isBreak
    val idleMinutes =
        (elapsedRealtime - timerUiState.endTime + WIGGLE_ROOM_MILLIS).milliseconds.inWholeMinutes

    Card(modifier = Modifier.wrapContentSize()) {
        Column(
            modifier =
                Modifier
                    .animateContentSize()
                    .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.main_this_session),
                style = MaterialTheme.typography.titleSmall.copy(MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.wrapContentHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        if (isBreak) stringResource(R.string.stats_break) else stringResource(R.string.stats_focus),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        timerUiState.completedMinutes.minutes.formatOverview(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }

                if (!isBreak) {
                    val interruptions = timerUiState.timeSpentPaused.milliseconds.inWholeMinutes
                    if (interruptions > 0) {
                        Column(
                            modifier = Modifier.wrapContentHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                stringResource(R.string.main_interruptions),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                interruptions.minutes.formatOverview(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }

                    if (idleMinutes > 0) {
                        Column(
                            modifier = Modifier.wrapContentHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text("Idle", style = MaterialTheme.typography.labelSmall)
                            Text(
                                idleMinutes.minutes.formatOverview(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
            if (!isBreak && idleMinutes > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = addIdleMinutes,
                        onCheckedChange = onAddIdleMinutesChanged,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.main_consider_idle_time_as_extra_focus),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(historyUiState: HistoryUiState) {
    if (historyUiState.todayWorkMinutes > 0 || historyUiState.todayBreakMinutes > 0) {
        Card(
            modifier =
                Modifier
                    .wrapContentSize()
                    .padding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .animateContentSize(),
            ) {
                Text(
                    stringResource(R.string.stats_today),
                    style = MaterialTheme.typography.titleSmall.copy(MaterialTheme.colorScheme.primary),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.wrapContentHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            stringResource(R.string.stats_focus),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            historyUiState.todayWorkMinutes.minutes.formatOverview(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Column(
                        modifier = Modifier.wrapContentHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            stringResource(R.string.stats_break),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            historyUiState.todayBreakMinutes.minutes.formatOverview(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    if (historyUiState.todayInterruptedMinutes > 0) {
                        Column(
                            modifier = Modifier.wrapContentHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                stringResource(R.string.main_interruptions),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                historyUiState.todayInterruptedMinutes.minutes.formatOverview(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FinishedSessionContentPreview() {
    FinishedSessionContent(
        timerUiState =
            TimerUiState(
                timerType = TimerType.WORK,
                completedMinutes = 25,
                timeSpentPaused = 3.minutes.inWholeMilliseconds,
            ),
        historyUiState =
            HistoryUiState(
                todayWorkMinutes = 90,
                todayBreakMinutes = 55,
                todayInterruptedMinutes = 3,
            ),
        elapsedRealtime = 3.minutes.inWholeMilliseconds,
        addIdleMinutes = false,
        onChangeAddIdleMinutes = {},
        notes = "Some notes",
        onNotesChanged = {},
    )
}
