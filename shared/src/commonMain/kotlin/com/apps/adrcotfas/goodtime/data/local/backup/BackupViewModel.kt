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
package com.apps.adrcotfas.goodtime.data.local.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val isPro: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val isCsvBackupInProgress: Boolean = false,
    val isJsonBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val backupResult: Boolean? = null,
    val restoreResult: Boolean? = null,
)

class BackupViewModel(
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.onStart { loadData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    private fun loadData() {
        viewModelScope.launch {
            settingsRepository.settings.map { it.isPro }.distinctUntilChanged().collect { isPro ->
                _uiState.update {
                    it.copy(isPro = isPro)
                }
            }
        }
    }

    fun backup() {
        coroutineScope.launch {
            _uiState.update { BackupUiState(isBackupInProgress = true) }
            backupManager.backup { success ->
                _uiState.update {
                    val backupResult = if (!success) false else null
                    it.copy(
                        backupResult = backupResult,
                    )
                }
            }
        }
    }

    fun backupToCsv() {
        coroutineScope.launch {
            _uiState.update { BackupUiState(isCsvBackupInProgress = true) }
            backupManager.backupToCsv { success ->
                _uiState.update {
                    val backupResult = if (!success) false else null
                    it.copy(
                        backupResult = backupResult,
                    )
                }
            }
        }
    }

    fun backupToJson() {
        coroutineScope.launch {
            _uiState.update { BackupUiState(isJsonBackupInProgress = true) }
            backupManager.backupToJson { success ->
                _uiState.update {
                    val backupResult = if (!success) false else null
                    it.copy(
                        backupResult = backupResult,
                    )
                }
            }
        }
    }

    fun restore() {
        coroutineScope.launch {
            _uiState.update { BackupUiState(isRestoreInProgress = true) }
            backupManager.restore { success ->
                _uiState.update {
                    it.copy(
                        isRestoreInProgress = false,
                        restoreResult = success,
                    )
                }
            }
        }
    }

    fun clearBackupError() = _uiState.update { it.copy(backupResult = null) }
    fun clearRestoreError() = _uiState.update { it.copy(restoreResult = null) }
    fun clearProgress() = _uiState.update {
        it.copy(
            isBackupInProgress = false,
            isRestoreInProgress = false,
            isCsvBackupInProgress = false,
            isJsonBackupInProgress = false,
        )
    }
}
