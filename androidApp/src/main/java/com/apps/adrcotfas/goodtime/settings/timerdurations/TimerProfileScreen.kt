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
package com.apps.adrcotfas.goodtime.settings.timerdurations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.settings.TimerProfileViewModel
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.DropdownMenuBox
import com.apps.adrcotfas.goodtime.ui.common.EditableNumberListItem
import com.apps.adrcotfas.goodtime.ui.common.InfoDialog
import com.apps.adrcotfas.goodtime.ui.common.SliderListItem
import com.apps.adrcotfas.goodtime.ui.common.TimerTypeRow
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Edit
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerProfileScreen(
    viewModel: TimerProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberScrollState()

    if (uiState.isLoading) return

    val label = uiState.tmpLabel
    val isCountDown = label.timerProfile.isCountdown
    val isBreakEnabled = label.timerProfile.isBreakEnabled
    val isLongBreakEnabled = label.timerProfile.isLongBreakEnabled

    val isDifferentFromDefault = uiState.defaultLabel != label

    var showBreakBudgetInfoDialog by remember { mutableStateOf(false) }

    var showAddTimerProfileDialog by remember { mutableStateOf(false) }
    var showTimerProfilesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.settings_timer_durations_title),
                onNavigateBack = onNavigateBack,
                icon = Icons.AutoMirrored.Default.ArrowBack,
                showSeparator = listState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(listState)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding(),
        ) {
            Column {
                AnimatedVisibility(uiState.timerProfiles.isNotEmpty()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                        ) {
                            DropdownMenuBox(
                                contentModifier = Modifier.fillMaxWidth(),
                                colored = true,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                value =
                                    label.timerProfile.name
                                        ?: stringResource(R.string.labels_custom),
                                options = uiState.timerProfiles.mapNotNull { it.name },
                                onDismissRequest = {},
                                onDropdownMenuItemSelected = {
                                    val selectedProfile = uiState.timerProfiles[it]
                                    viewModel.updateTmpLabel(
                                        label.copy(timerProfile = selectedProfile),
                                        resetProfile = false,
                                    )
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                showTimerProfilesSheet = true
                            },
                        ) {
                            Icon(
                                imageVector = EvaIcons.Outline.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                TimerTypeRow(
                    isCountDown = isCountDown,
                    onCountDownEnabled = {
                        viewModel.updateTmpLabel(
                            label.copy(
                                timerProfile = label.timerProfile.copy(isCountdown = it),
                            ),
                        )
                    },
                )
                AnimatedVisibility(isCountDown) {
                    Column {
                        EditableNumberListItem(
                            title = stringResource(R.string.labels_focus_time),
                            value = label.timerProfile.workDuration,
                            onValueChange = {
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile = label.timerProfile.copy(workDuration = it),
                                    ),
                                )
                            },
                        )
                        EditableNumberListItem(
                            title = stringResource(R.string.labels_break_time),
                            value = label.timerProfile.breakDuration,
                            onValueChange = {
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile = label.timerProfile.copy(breakDuration = it),
                                    ),
                                )
                            },
                            enableSwitch = true,
                            switchValue = isBreakEnabled,
                            onSwitchChange = {
                                val longBreakState = if (!it) false else isLongBreakEnabled
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile =
                                            label.timerProfile.copy(
                                                isBreakEnabled = it,
                                                isLongBreakEnabled = longBreakState,
                                            ),
                                    ),
                                )
                            },
                        )
                        EditableNumberListItem(
                            title = stringResource(R.string.labels_long_break),
                            value = label.timerProfile.longBreakDuration,
                            onValueChange = {
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile =
                                            label.timerProfile.copy(
                                                longBreakDuration = it,
                                            ),
                                    ),
                                )
                            },
                            enabled = isBreakEnabled,
                            enableSwitch = true,
                            switchValue = isLongBreakEnabled,
                            onSwitchChange = {
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile =
                                            label.timerProfile.copy(
                                                isLongBreakEnabled = it,
                                            ),
                                    ),
                                )
                            },
                        )
                        EditableNumberListItem(
                            title = stringResource(R.string.labels_sessions_before_long_break),
                            value = label.timerProfile.sessionsBeforeLongBreak,
                            minValue = 2,
                            maxValue = 8,
                            enabled = isBreakEnabled && isLongBreakEnabled,
                            onValueChange = {
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile =
                                            label.timerProfile.copy(
                                                sessionsBeforeLongBreak = it,
                                            ),
                                    ),
                                )
                            },
                        )
                    }
                }
                AnimatedVisibility(!isCountDown) {
                    Column {
                        val toggleBreak = {
                            viewModel.updateTmpLabel(
                                label.copy(
                                    timerProfile = label.timerProfile.copy(isBreakEnabled = !isBreakEnabled),
                                ),
                            )
                        }
                        ListItem(
                            modifier =
                                Modifier.toggleable(
                                    value = isBreakEnabled,
                                    onValueChange = { toggleBreak() },
                                ),
                            headlineContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp,
                                            Alignment.Start,
                                        ),
                                ) {
                                    Text(stringResource(R.string.labels_enable_break_budget))

                                    IconButton(
                                        onClick = {
                                            showBreakBudgetInfoDialog = true
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = stringResource(R.string.labels_break_budget_info),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isBreakEnabled,
                                    onCheckedChange = null,
                                )
                            },
                        )
                        SliderListItem(
                            title = stringResource(R.string.labels_focus_break_ratio),
                            min = 2,
                            max = 6,
                            enabled = isBreakEnabled,
                            value = label.timerProfile.workBreakRatio,
                            showValue = true,
                            onValueChange = {
                                viewModel.updateTmpLabel(
                                    label.copy(
                                        timerProfile =
                                            label.timerProfile.copy(
                                                workBreakRatio = it,
                                            ),
                                    ),
                                )
                            },
                        )
                    }
                }

                AnimatedVisibility(isDifferentFromDefault || label.timerProfile.name == null) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val createProfileButtonTargetWeight =
                            if (label.timerProfile.name == null) 1f else 0f
                        val createProfileButtonAnimatedWeight by animateFloatAsState(
                            targetValue = createProfileButtonTargetWeight,
                            label = "createProfileButtonWeight",
                        )

                        val saveButtonTargetWeight =
                            if (isDifferentFromDefault) 1f else 0f
                        val saveButtonAnimatedWeight by animateFloatAsState(
                            targetValue = saveButtonTargetWeight,
                            label = "createProfileButtonWeight",
                        )

                        if (createProfileButtonAnimatedWeight > 0.5f) {
                            FilledTonalButton(
                                enabled = uiState.isPro,
                                modifier = Modifier.weight(createProfileButtonAnimatedWeight),
                                colors =
                                    ButtonDefaults.filledTonalButtonColors().copy(
                                        containerColor =
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.18f,
                                            ),
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                onClick = { showAddTimerProfileDialog = true },
                            ) {
                                Text(stringResource(R.string.settings_create_profile))
                            }
                        }

                        if (saveButtonAnimatedWeight > 0.5f) {
                            Button(
                                modifier = Modifier.weight(saveButtonAnimatedWeight),
                                enabled = isDifferentFromDefault,
                                onClick = { viewModel.saveChanges(label = uiState.tmpLabel) },
                            ) {
                                Text(stringResource(R.string.main_save))
                            }
                        }
                    }
                }
            }
        }
        if (showBreakBudgetInfoDialog) {
            InfoDialog(
                title = stringResource(R.string.labels_break_budget_info),
                subtitle =
                    "${stringResource(R.string.labels_break_budget_desc1)}\n" +
                        stringResource(R.string.labels_break_budget_desc2),
            ) {
                showBreakBudgetInfoDialog = false
            }
        }
        if (showAddTimerProfileDialog) {
            CreateTimerProfileDialog(
                profileNames = uiState.timerProfiles.mapNotNull { it.name },
                onConfirm = {
                    val timerProfile = label.timerProfile.copy(name = it)
                    viewModel.createTimerProfile(timerProfile)
                    val newLabel = label.copy(timerProfile = timerProfile)
                    viewModel.updateTmpLabel(
                        newLabel = newLabel,
                        resetProfile = false,
                    )
                    viewModel.saveChanges(newLabel)
                    showAddTimerProfileDialog = false
                },
                onDismiss = {
                    showAddTimerProfileDialog = false
                },
            )
        }
        if (showTimerProfilesSheet && uiState.timerProfiles.isNotEmpty()) {
            TimerProfileBottomSheet(
                profiles = uiState.timerProfiles,
                sheetState = sheetState,
                onDismiss = { showTimerProfilesSheet = false },
                onDelete = viewModel::deleteTimerProfile,
            )
        }
    }
}
