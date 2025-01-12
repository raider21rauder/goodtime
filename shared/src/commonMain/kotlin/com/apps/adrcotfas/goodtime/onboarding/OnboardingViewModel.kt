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
package com.apps.adrcotfas.goodtime.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(val loading: Boolean, val finished: Boolean)

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _onboardingState =
        MutableStateFlow(OnboardingUiState(loading = true, finished = false))
    val onboardingState: StateFlow<OnboardingUiState> = _onboardingState

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.onboardingFinished }
                .collect { onboardingFinished ->
                    _onboardingState.update {
                        it.copy(
                            loading = false,
                            finished = onboardingFinished,
                        )
                    }
                }
        }
    }

    fun setOnboardingFinished(finished: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnboardingFinished(finished)
        }
    }
}
