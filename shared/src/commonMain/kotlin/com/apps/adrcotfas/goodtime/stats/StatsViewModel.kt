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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.SessionOverviewData
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_DURATION
import com.apps.adrcotfas.goodtime.data.model.toExternal
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

data class StatsUiState(
    val labels: List<Label> = emptyList(),
    val selectedLabels: List<String> = emptyList(),
    val overviewData: SessionOverviewData = SessionOverviewData(),

    // Selection UI related fields
    val selectedSessions: List<Long> = emptyList(),
    val unselectedSessions: List<Long> = emptyList(), // for the case with select all active
    val selectedSessionsCountWhenAllSelected: Int = 0,
    val isSelectAllEnabled: Boolean = false,
    val selectedLabelToBulkEdit: String? = null,

    val sessionToEdit: Session? = null, // this does not change after initialization
    val newSession: Session = Session.default(),
    val showAddSession: Boolean = false,
    val canSave: Boolean = true,
) {
    val showSelectionUi: Boolean
        get() = selectedSessions.isNotEmpty() || isSelectAllEnabled

    val selectionCount: Int
        get() =
            if (isSelectAllEnabled) {
                selectedSessionsCountWhenAllSelected - unselectedSessions.size
            } else {
                selectedSessions.size
            }
}

class StatsViewModel(
    private val localDataRepo: LocalDataRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState
        .onStart { loadData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    val pagedSessions: Flow<PagingData<Session>> =
        uiState.map { it.selectedLabels }.flatMapLatest { selectSessionsForHistoryPaged(it) }

    private fun selectSessionsForHistoryPaged(labels: List<String>): Flow<PagingData<Session>> =
        Pager(PagingConfig(pageSize = 50, prefetchDistance = 50)) {
            localDataRepo.selectSessionsForHistoryPaged(labels)
        }.flow.map { value ->
            value.map {
                it.toExternal()
            }
        }

    private fun loadData() {
        viewModelScope.launch {
            //TODO: VieModel is not cleared / https://issuetracker.google.com/issues/390201791
            val labels = localDataRepo.selectLabelsByArchived(isArchived = false).first()
            _uiState.update {
                it.copy(labels = labels, selectedLabels = labels.map { label -> label.name })
            }
            combine(
                settingsRepository.settings.distinctUntilChanged { old, new ->
                    old.firstDayOfWeek == new.firstDayOfWeek && old.workdayStart == new.workdayStart
                },
                uiState.map { it.selectedLabels },
            ) { settings, selectedLabels ->
                val todayStart = timeProvider.startOfTodayAdjusted(settings.workdayStart)
                val weekStart = timeProvider.startOfThisWeekAdjusted(
                    DayOfWeek(settings.firstDayOfWeek),
                    settings.workdayStart,
                )
                val monthStart = timeProvider.startOfThisMonthAdjusted(settings.workdayStart)
                localDataRepo.selectOverviewAfter(todayStart, weekStart, monthStart, selectedLabels)
                    .first()
            }.collect { overviewData ->
                _uiState.update {
                    it.copy(overviewData = overviewData)
                }
            }
        }
    }

    fun setSelectedLabels(selectedLabels: List<String>) {
        _uiState.update { it.copy(selectedLabels = selectedLabels) }
    }

    fun toggleSessionIsSelected(index: Long) {
        _uiState.update {
            if (it.isSelectAllEnabled) {
                val unselectedSessions = it.unselectedSessions.toMutableList()
                if (unselectedSessions.contains(index)) {
                    unselectedSessions.remove(index)
                } else {
                    unselectedSessions.add(index)
                }
                it.copy(
                    unselectedSessions = unselectedSessions,
                    isSelectAllEnabled = unselectedSessions.size != it.selectedSessionsCountWhenAllSelected,
                )
            } else {
                val selectedSessions = it.selectedSessions.toMutableList()
                if (selectedSessions.contains(index)) {
                    selectedSessions.remove(index)
                } else {
                    selectedSessions.add(index)
                }
                it.copy(selectedSessions = selectedSessions)
            }
        }
    }

    fun clearShowSelectionUi() {
        _uiState.update {
            it.copy(
                isSelectAllEnabled = false,
                selectedSessions = emptyList(),
                unselectedSessions = emptyList(),
                selectedSessionsCountWhenAllSelected = 0,
            )
        }
    }

    fun selectAllSessions(allSessionsCount: Int) {
        _uiState.update {
            it.copy(
                isSelectAllEnabled = true,
                selectedSessionsCountWhenAllSelected = allSessionsCount,
                selectedSessions = emptyList(),
                unselectedSessions = emptyList(),
            )
        }
    }

    fun deleteSelectedSessions() {
        viewModelScope.launch {
            if (uiState.value.isSelectAllEnabled) {
                localDataRepo.deleteSessionsExcept(
                    uiState.value.unselectedSessions,
                    uiState.value.selectedLabels,
                )
            } else {
                localDataRepo.deleteSessions(uiState.value.selectedSessions)
            }
        }
    }

    fun updateSessionToEdit(session: Session) {
        _uiState.update { state ->
            state.copy(newSession = session)
        }
    }

    fun setCanSave(isValid: Boolean) {
        _uiState.update { it.copy(canSave = isValid) }
    }

    fun saveSession() {
        viewModelScope.launch {
            val newSession = uiState.value.newSession
            val sessionToEditId = uiState.value.sessionToEdit?.id
            sessionToEditId?.let {
                localDataRepo.updateSession(newSession.id, newSession)
            } ?: localDataRepo.insertSession(newSession)
        }
    }

    fun onAddEditSession(sessionToEdit: Session? = null) {
        val session = sessionToEdit ?: generateNewSession()
        _uiState.update {
            it.copy(
                sessionToEdit = sessionToEdit,
                newSession = session,
                showAddSession = true,
            )
        }
    }

    fun clearAddEditSession() {
        _uiState.update { it.copy(showAddSession = false) }
    }

    private fun generateNewSession(): Session {
        return Session.create(
            duration = DEFAULT_WORK_DURATION.toLong(),
            timestamp = timeProvider.now(),
            interruptions = 0,
            label = Label.DEFAULT_LABEL_NAME,
            isWork = true,
        )
    }

    fun setSelectedLabelToBulkEdit(label: String) {
        _uiState.update { it.copy(selectedLabelToBulkEdit = label) }
    }

    fun bulkEditLabel() {
        viewModelScope.launch {
            val label = uiState.value.selectedLabelToBulkEdit
            label?.let {
                if (uiState.value.isSelectAllEnabled) {
                    localDataRepo.updateSessionsLabelByIdsExcept(
                        label,
                        uiState.value.unselectedSessions,
                        uiState.value.selectedLabels,
                    )
                } else {
                    localDataRepo.updateSessionsLabelByIds(label, uiState.value.selectedSessions)
                }
            }
        }
    }
}
