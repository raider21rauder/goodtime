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
package com.apps.adrcotfas.goodtime.stats

import android.text.format.DateFormat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.ui.common.DatePickerDialog
import com.apps.adrcotfas.goodtime.ui.common.DragHandle
import com.apps.adrcotfas.goodtime.ui.common.TimePicker
import com.apps.adrcotfas.goodtime.ui.common.toLocalTime
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

private enum class TabType {
    Overview, History
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = koinViewModel()) {
    val context = LocalContext.current

    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showBottomSheet = uiState.showAddSession

    Scaffold(
        modifier = Modifier
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .windowInsetsPadding(
                WindowInsets.statusBars,
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics") },
                scrollBehavior = topAppBarScrollBehavior,
                actions = {
                    IconButton(onClick = {
                        viewModel.onAddEditSession()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add new session",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->

        paddingValues

        var type by rememberSaveable { mutableStateOf(TabType.Overview) }
        val titles = listOf("Overview", "History")
        var showDatePicker by rememberSaveable { mutableStateOf(false) }
        var showTimePicker by rememberSaveable { mutableStateOf(false) }
        var showLabelPicker by rememberSaveable { mutableStateOf(false) }

        // TODO: add session button
        // TODO: select labels button with badge according to number of selected labels
        HistoryTab(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            sessions = uiState.sessions.filter { it.isWork },
            labels = uiState.labels,
            onClick = { index ->
                viewModel.onAddEditSession(uiState.sessions.first { session -> session.id == index })
            },
            onLongClick = {
                viewModel.toggleIsSelected(it)
            },
        )

        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()

        val hideSheet = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    viewModel.clearAddEditSession()
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { hideSheet() },
                sheetState = sheetState,
                dragHandle = {
                    DragHandle(
                        buttonText = "Save",
                        isEnabled = uiState.canSave,
                        onClose = { hideSheet() },
                        onClick = {
                            viewModel.saveSession()
                            hideSheet()
                        },
                    )
                },
            ) {
                AddEditSessionContent(
                    session = uiState.newSession,
                    labels = uiState.labels,
                    onUpdate = {
                        viewModel.updateSessionToEdit(it)
                    },
                    onValidate = {
                        viewModel.setCanSave(it)
                    },
                    onOpenLabelSelector = { showLabelPicker = true },
                    onOpenDatePicker = { showDatePicker = true },
                    onOpenTimePicker = { showTimePicker = true },
                )
            }
        }
        if (showDatePicker) {
            val dateTime = Instant.fromEpochMilliseconds(uiState.newSession.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val tomorrowMillis =
                LocalDateTime(
                    now.date.plus(DatePeriod(days = 1)),
                    LocalTime(hour = 0, minute = 0),
                ).toInstant(
                    TimeZone.currentSystemDefault(),
                ).toEpochMilliseconds()

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dateTime.toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis < tomorrowMillis
                },
            )
            DatePickerDialog(
                onDismiss = { showDatePicker = false },
                onConfirm = {
                    val newDate = Instant.fromEpochMilliseconds(it)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val newDateTime = LocalDateTime(newDate.date, dateTime.time)
                    val newTimestamp =
                        newDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    viewModel.updateSessionToEdit(
                        uiState.newSession.copy(
                            timestamp = newTimestamp,
                        ),
                    )
                    showDatePicker = false
                },
                datePickerState = datePickerState,
            )
        }
        if (showTimePicker) {
            val dateTime = Instant.fromEpochMilliseconds(uiState.newSession.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val time = dateTime.time
            val timePickerState = rememberTimePickerState(
                initialHour = time.hour,
                initialMinute = time.minute,
                is24Hour = DateFormat.is24HourFormat(context),
            )
            TimePicker(
                onDismiss = { showTimePicker = false },
                onConfirm = {
                    val newTime = it.toLocalTime()
                    val newDateTime = LocalDateTime(dateTime.date, newTime)
                    val newTimestamp =
                        newDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

                    viewModel.updateSessionToEdit(
                        uiState.newSession.copy(
                            timestamp = newTimestamp,
                        ),
                    )
                    showTimePicker = false
                },
                timePickerState = timePickerState,
            )
        }
//        Column {
//            SecondaryTabRow(
//                selectedTabIndex = type.ordinal,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues),
//            ) {
//                titles.forEachIndexed { index, title ->
//                    Tab(
//                        selected = type == TabType.entries[index],
//                        onClick = { type = TabType.entries[index] },
//                        text = { Text(title) },
//                    )
//                }
//                when (type) {
//                    TabType.Overview -> OverviewTab()
//                    TabType.History -> HistoryTab(sessions = uiState.sessions, labels = uiState.labels)
//                }
//            }
//        }
    }
}
