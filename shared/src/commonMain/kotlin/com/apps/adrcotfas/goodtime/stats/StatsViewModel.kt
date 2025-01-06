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
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_DURATION
import com.apps.adrcotfas.goodtime.data.model.toExternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val labels: List<Label> = emptyList(),
    val selectedLabels: List<String> = emptyList(),
    val selectedSessions: List<Long> = emptyList(),
    val showLabelSelection: Boolean = false,
    val sessionToEdit: Session? = null, // this does not change after initialization
    val newSession: Session = Session.default(),
    val showAddSession: Boolean = false,
    val canSave: Boolean = true,
)

class StatsViewModel(
    private val localDataRepo: LocalDataRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState
        .onStart { loadData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    val sessions: Flow<PagingData<Session>> = uiState.map { it.selectedLabels }.flatMapLatest { selectSessionsForHistoryPaged(it) }

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
            localDataRepo.selectLabelsByArchived(isArchived = false).first().let { labels ->
                _uiState.update {
                    it.copy(
                        labels = labels,
                        selectedLabels = labels.map { label -> label.name },
                    )
                }
            }
        }
    }

    fun toggleSessionIsSelected(index: Long) {
        _uiState.update {
            val selectedSessions = it.selectedSessions.toMutableList()
            if (selectedSessions.contains(index)) {
                selectedSessions.remove(index)
            } else {
                selectedSessions.add(index)
            }
            it.copy(selectedSessions = selectedSessions)
        }
    }

    fun selectLabels(selectedLabels: List<String>) {
        _uiState.update { it.copy(selectedLabels = selectedLabels) }
    }

    fun toggleSelectedSession(index: Long) {
        _uiState.update {
            val selectedSessions = it.selectedSessions.toMutableList()
            if (selectedSessions.contains(index)) {
                selectedSessions.remove(index)
            } else {
                selectedSessions.add(index)
            }
            it.copy(selectedSessions = selectedSessions)
        }
    }

    fun deleteSession(index: Long) {
        viewModelScope.launch {
            localDataRepo.deleteSession(index)
            if (uiState.value.selectedSessions.contains(index)) {
                _uiState.update { it.copy(selectedSessions = it.selectedSessions - index) }
            }
        }
    }

    fun setShowLabelSelection(show: Boolean) {
        _uiState.update { it.copy(showLabelSelection = show) }
    }

    fun setShowAddEditSession(session: Session) {
        _uiState.update {
            it.copy(
                sessionToEdit = session,
                newSession = session,
            )
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
}
