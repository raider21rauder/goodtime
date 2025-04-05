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
package com.apps.adrcotfas.goodtime.labels.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.LABEL_NAME_MAX_LENGTH
import com.apps.adrcotfas.goodtime.data.model.isDefault
import com.apps.adrcotfas.goodtime.labels.AddEditLabelViewModel
import com.apps.adrcotfas.goodtime.labels.labelNameIsValid
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.ColorSelectRow
import com.apps.adrcotfas.goodtime.ui.common.EditableNumberListItem
import com.apps.adrcotfas.goodtime.ui.common.InfoDialog
import com.apps.adrcotfas.goodtime.ui.common.SliderListItem
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.common.clearFocusOnKeyboardDismiss
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Navigation2
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLabelScreen(
    viewModel: AddEditLabelViewModel = koinViewModel(),
    labelName: String,
    onNavigateToDefault: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isEditMode = labelName.isNotEmpty()
    val context = LocalContext.current

    val listState = rememberScrollState()

    LaunchedEffect(labelName) {
        val defaultLabelName = context.getString(R.string.labels_default_label_name)
        viewModel.init(labelName, defaultLabelName)
    }

    if (uiState.isLoading) return

    val label = uiState.newLabel
    val isDefaultLabel = label.isDefault()
    val labelNameToDisplay = if (isDefaultLabel) uiState.defaultLabelDisplayName else label.name

    val followDefault = label.useDefaultTimeProfile
    val isCountDown = label.timerProfile.isCountdown
    val isBreakEnabled = label.timerProfile.isBreakEnabled
    val isLongBreakEnabled = label.timerProfile.isLongBreakEnabled

    var showBreakBudgetInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                onNavigateBack = onNavigateBack,
                icon = Icons.Default.Close,
                actions = {
                    if (isDefaultLabel && !label.isSameAs(Label.defaultLabel())) {
                        Button(
                            modifier = Modifier
                                .wrapContentSize()
                                .heightIn(min = 32.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.12f,
                                ),
                            ),
                            onClick = { viewModel.setNewLabel(Label.defaultLabel()) },
                        ) {
                            Text(stringResource(R.string.main_reset_to_default))
                        }
                    }
                    if (uiState.labelToEdit != label) {
                        SaveButton(
                            labelName,
                            label,
                            uiState.labelNameIsValid(),
                            isEditMode,
                            viewModel::updateLabel,
                            viewModel::addLabel,
                            onNavigateBack,
                        )
                    }
                },
                showSeparator = listState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(listState)
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
        ) {
            if (!isDefaultLabel) {
                LabelNameRow(
                    isAddingNewLabel = !isEditMode,
                    labelName = labelNameToDisplay,
                    onValueChange = {
                        val newLabelName = it
                        viewModel.setNewLabel(
                            uiState.newLabel.copy(name = newLabelName),
                        )
                    },
                    showError = !uiState.labelNameIsValid(),
                )
                ColorSelectRow(
                    selectedIndex = label.colorIndex.toInt(),
                ) {
                    viewModel.setNewLabel(label.copy(colorIndex = it.toLong()))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (!isDefaultLabel) {
                ListItem(
                    modifier = Modifier.clickable {
                        viewModel.setNewLabel(label.copy(useDefaultTimeProfile = !followDefault))
                    },
                    leadingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalIconButton(onClick = {
                                onNavigateToDefault()
                            }) {
                                Icon(
                                    imageVector = EvaIcons.Outline.Navigation2,
                                    contentDescription = stringResource(R.string.labels_default_label_name),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            VerticalDivider(modifier = Modifier.height(32.dp))
                        }
                    },
                    headlineContent = {
                        Text(stringResource(R.string.labels_follow_default_time_profile))
                    },
                    trailingContent = {
                        Switch(
                            checked = followDefault,
                            onCheckedChange = {
                                viewModel.setNewLabel(label.copy(useDefaultTimeProfile = it))
                            },
                        )
                    },
                )
            }
            AnimatedVisibility(isDefaultLabel || !followDefault) {
                Column {
                    TimerTypeRow(isCountDown = isCountDown, onCountDownEnabled = {
                        viewModel.setNewLabel(
                            label.copy(
                                timerProfile = label.timerProfile.copy(isCountdown = it),
                            ),
                        )
                    })
                    if (isCountDown) {
                        Column {
                            EditableNumberListItem(
                                title = stringResource(R.string.labels_focus_time),
                                value = label.timerProfile.workDuration,
                                onValueChange = {
                                    viewModel.setNewLabel(
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
                                    viewModel.setNewLabel(
                                        label.copy(
                                            timerProfile = label.timerProfile.copy(breakDuration = it),
                                        ),
                                    )
                                },
                                enableSwitch = true,
                                switchValue = isBreakEnabled,
                                onSwitchChange = {
                                    val longBreakState = if (!it) false else isLongBreakEnabled
                                    viewModel.setNewLabel(
                                        label.copy(
                                            timerProfile = label.timerProfile.copy(
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
                                    viewModel.setNewLabel(
                                        label.copy(
                                            timerProfile = label.timerProfile.copy(
                                                longBreakDuration = it,
                                            ),
                                        ),
                                    )
                                },
                                enabled = isBreakEnabled,
                                enableSwitch = true,
                                switchValue = isLongBreakEnabled,
                                onSwitchChange = {
                                    viewModel.setNewLabel(
                                        label.copy(
                                            timerProfile = label.timerProfile.copy(
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
                                    viewModel.setNewLabel(
                                        label.copy(
                                            timerProfile = label.timerProfile.copy(
                                                sessionsBeforeLongBreak = it,
                                            ),
                                        ),
                                    )
                                },
                            )
                        }
                    } else {
                        Column {
                            val toggleBreak = {
                                viewModel.setNewLabel(
                                    label.copy(
                                        timerProfile = label.timerProfile.copy(isBreakEnabled = !isBreakEnabled),
                                    ),
                                )
                            }
                            ListItem(
                                modifier = Modifier.toggleable(
                                    value = isBreakEnabled,
                                    onValueChange = { toggleBreak() },
                                ),
                                headlineContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(
                                            8.dp,
                                            Alignment.Start,
                                        ),
                                    ) {
                                        Text(stringResource(R.string.labels_enable_break_budget))

                                        IconButton(onClick = {
                                            showBreakBudgetInfoDialog = true
                                        }) {
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
                                    viewModel.setNewLabel(
                                        label.copy(
                                            timerProfile = label.timerProfile.copy(
                                                workBreakRatio = it,
                                            ),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
        if (showBreakBudgetInfoDialog) {
            InfoDialog(
                title = stringResource(R.string.labels_break_budget_info),
                subtitle = "${stringResource(R.string.labels_break_budget_desc1)}\n" +
                    stringResource(R.string.labels_break_budget_desc2),
            ) {
                showBreakBudgetInfoDialog = false
            }
        }
    }
}

@Composable
private fun LabelNameRow(
    isAddingNewLabel: Boolean,
    labelName: String,
    onValueChange: (String) -> Unit,
    showError: Boolean,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .animateContentSize(),
    ) {
        val internalModifier =
            if (isAddingNewLabel) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            }

        Box {
            BasicTextField(
                modifier = internalModifier
                    .fillMaxWidth()
                    .clearFocusOnKeyboardDismiss(),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                value = labelName,
                onValueChange = {
                    if (it.length <= LABEL_NAME_MAX_LENGTH) {
                        onValueChange(it)
                    }
                },
            )
            if (labelName.isEmpty()) {
                Text(
                    text = stringResource(
                        R.string.labels_add_label_name,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(showError && labelName.isNotEmpty()) {
            Text(
                text = stringResource(R.string.labels_label_name_already_exists),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        LaunchedEffect(labelName) {
            if (isAddingNewLabel) focusRequester.requestFocus()
        }
    }
}

@Composable
private fun TimerTypeRow(isCountDown: Boolean, onCountDownEnabled: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FilterChip(
            onClick = { onCountDownEnabled(true) },
            label = {
                Text(stringResource(R.string.labels_countdown))
            },
            selected = isCountDown,
        )

        FilterChip(
            onClick = { onCountDownEnabled(false) },
            label = {
                Text(stringResource(R.string.labels_count_up))
            },
            selected = !isCountDown,
        )
    }
}

@Composable
fun SaveButton(
    labelToEditInitialName: String,
    labelToEdit: Label,
    hasValidName: Boolean,
    isEditMode: Boolean,
    onUpdate: (String, Label) -> Unit,
    onAdd: (Label) -> Unit,
    onSave: () -> Unit,
) {
    Button(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .padding(horizontal = 8.dp),
        enabled = hasValidName,
        onClick = {
            if (isEditMode) {
                onUpdate(labelToEditInitialName, labelToEdit)
            } else {
                onAdd(labelToEdit)
            }
            onSave()
        },
    ) {
        Text(stringResource(R.string.main_save))
    }
}
