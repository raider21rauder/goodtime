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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.apps.adrcotfas.goodtime.ui.common.ConfirmationDialog
import com.apps.adrcotfas.goodtime.ui.common.DatePickerDialog
import com.apps.adrcotfas.goodtime.ui.common.DragHandle
import com.apps.adrcotfas.goodtime.ui.common.IconButtonWithBadge
import com.apps.adrcotfas.goodtime.ui.common.SelectLabelDialog
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TimePicker
import com.apps.adrcotfas.goodtime.ui.common.toLocalTime
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.map
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
    Overview, Timeline
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = koinViewModel(),
    historyViewModel: StatisticsHistoryViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoadingHistoryChartData by historyViewModel.uiState.map { it.isLoading }
        .collectAsStateWithLifecycle(true)
    val sessionsPagingItems = viewModel.pagedSessions.collectAsLazyPagingItems()
    val selectedLabelsCount = uiState.selectedLabels.size
    val historyListState = rememberLazyListState()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showSelectVisibleLabelsDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectLabelDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var showEditBulkLabelDialog by rememberSaveable { mutableStateOf(false) }
    var showEditLabelConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    val isLoading = uiState.isLoading || isLoadingHistoryChartData

    BackHandler(enabled = uiState.showSelectionUi) {
        if (uiState.showSelectionUi) {
            viewModel.clearShowSelectionUi()
        }
    }

    Scaffold(
        topBar = {
            StatisticsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onAddButtonClick = { viewModel.onAddEditSession() },
                onLabelButtonClick = {
                    if (uiState.showSelectionUi) {
                        showEditBulkLabelDialog = true
                    } else {
                        showSelectVisibleLabelsDialog = true
                    }
                },
                selectedLabelsCount = selectedLabelsCount,
                onCancel = { viewModel.clearShowSelectionUi() },
                onDeleteClick = { showDeleteConfirmationDialog = true },
                onSelectAll = { viewModel.selectAllSessions(sessionsPagingItems.itemCount) },
                showSelectionUi = uiState.showSelectionUi,
                selectionCount = uiState.selectionCount,
                showSeparator = uiState.showSelectionUi && historyListState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        var type by rememberSaveable { mutableStateOf(TabType.Overview) }
        val titles = listOf("Overview", "Timeline")

        Crossfade(isLoading) { isLoading ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier.padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding(),
                    ),
                ) {
                    AnimatedVisibility(!uiState.showSelectionUi) {
                        SecondaryTabRow(
                            selectedTabIndex = type.ordinal,
                            modifier = Modifier.wrapContentSize(),
                            divider = { SubtleHorizontalDivider() },
                        ) {
                            titles.forEachIndexed { index, title ->
                                Tab(
                                    selected = type == TabType.entries[index],
                                    onClick = { type = TabType.entries[index] },
                                    text = { Text(title) },
                                )
                            }
                        }
                    }

                    when (type) {
                        TabType.Overview -> OverviewTab(
                            firstDayOfWeek = uiState.firstDayOfWeek,
                            workDayStart = uiState.workDayStart,
                            statisticsSettings = uiState.statisticsSettings,
                            statisticsData = uiState.statisticsData,
                            onChangeOverviewType = {
                                viewModel.setOverviewType(it)
                            },
                            onChangeOverviewDurationType = {
                                viewModel.setOverviewDurationType(it)
                            },
                            onChangePieChartOverviewType = {
                                viewModel.setPieChartViewType(it)
                            },
                            historyChartViewModel = historyViewModel,
                        )

                        TabType.Timeline -> {
                            TimelineTab(
                                listState = historyListState,
                                sessions = sessionsPagingItems,
                                isSelectAllEnabled = uiState.isSelectAllEnabled,
                                selectedSessions = uiState.selectedSessions,
                                unselectedSessions = uiState.unselectedSessions,
                                labels = uiState.labels,
                                onClick = { session ->
                                    if (uiState.showSelectionUi) {
                                        viewModel.toggleSessionIsSelected(session.id)
                                    } else {
                                        viewModel.onAddEditSession(session)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSessionIsSelected(it.id)
                                },
                            )
                        }
                    }
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                val hideSheet = { viewModel.clearAddEditSession() }

                if (uiState.showAddSession) {
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
                            onOpenLabelSelector = { showSelectLabelDialog = true },
                            onOpenDatePicker = { showDatePicker = true },
                            onOpenTimePicker = { showTimePicker = true },
                        )
                    }
                }
                if (showDatePicker) {
                    val dateTime = Instant.fromEpochMilliseconds(uiState.newSession.timestamp)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val now =
                        Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
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
                                newDateTime.toInstant(TimeZone.currentSystemDefault())
                                    .toEpochMilliseconds()
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
                                newDateTime.toInstant(TimeZone.currentSystemDefault())
                                    .toEpochMilliseconds()

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
                if (showSelectLabelDialog) {
                    SelectLabelDialog(
                        title = "Select label",
                        labels = uiState.labels,
                        initialSelectedLabels = persistentListOf(uiState.newSession.label),
                        onDismiss = { showSelectLabelDialog = false },
                        singleSelection = true,
                        onConfirm = {
                            viewModel.updateSessionToEdit(
                                uiState.newSession.copy(
                                    label = it.first(),
                                ),
                            )
                            showSelectLabelDialog = false
                        },
                    )
                }
                if (showSelectVisibleLabelsDialog) {
                    SelectLabelDialog(
                        title = "Select labels",
                        labels = uiState.labels,
                        initialSelectedLabels = uiState.selectedLabels,
                        onDismiss = { showSelectVisibleLabelsDialog = false },
                        singleSelection = false,
                        onConfirm = {
                            viewModel.setSelectedLabels(it)

                            val labelData = uiState.labels.filter { label ->
                                it.contains(label.name)
                            }.map { label -> LabelData(label.name, label.colorIndex) }

                            historyViewModel.setSelectedLabels(labelData)
                            showSelectVisibleLabelsDialog = false
                        },
                    )
                }
                if (showDeleteConfirmationDialog) {
                    ConfirmationDialog(
                        title = "Delete selected sessions?",
                        onDismiss = { showDeleteConfirmationDialog = false },
                        onConfirm = {
                            viewModel.deleteSelectedSessions()
                            viewModel.clearShowSelectionUi()
                            showDeleteConfirmationDialog = false
                        },
                    )
                }
                if (showEditBulkLabelDialog) {
                    SelectLabelDialog(
                        title = "Edit label",
                        labels = uiState.labels,
                        onDismiss = { showEditBulkLabelDialog = false },
                        singleSelection = true,
                        onConfirm = {
                            viewModel.setSelectedLabelToBulkEdit(it.first())
                            showEditBulkLabelDialog = false
                            showEditLabelConfirmationDialog = true
                        },
                    )
                }
                if (showEditLabelConfirmationDialog) {
                    ConfirmationDialog(
                        title = "Change label of selected sessions?",
                        onDismiss = { showEditLabelConfirmationDialog = false },
                        onConfirm = {
                            viewModel.bulkEditLabel()
                            viewModel.clearShowSelectionUi()
                            showEditLabelConfirmationDialog = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SelectLabelButton(count: Int, onClick: () -> Unit) {
    IconButtonWithBadge(
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Label,
                contentDescription = "Navigate to archived labels",
            )
        },
        count = count,
        onClick = onClick,
    )
}
