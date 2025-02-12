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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.LABEL_NAME_MAX_LENGTH
import com.apps.adrcotfas.goodtime.data.model.isDefault
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.labels.main.labelNameIsValid
import com.apps.adrcotfas.goodtime.ui.common.EditableNumberListItem
import com.apps.adrcotfas.goodtime.ui.common.SliderListItem
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.common.clearFocusOnKeyboardDismiss
import com.apps.adrcotfas.goodtime.ui.localColorsPalette
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLabelScreen(
    viewModel: LabelsViewModel = koinViewModel(),
    labelName: String,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isEditMode = labelName.isNotEmpty()
    val context = LocalContext.current

    val listState = rememberScrollState()

    LaunchedEffect(labelName) {
        val defaultLabelName = context.getString(R.string.label_default)
        viewModel.init(labelName, defaultLabelName)
    }

    val label = uiState.newLabel
    val isDefaultLabel = label.isDefault()
    val labelNameToDisplay = if (isDefaultLabel) uiState.defaultLabelDisplayName else label.name

    val followDefault = label.useDefaultTimeProfile
    val isCountDown = label.timerProfile.isCountdown
    val isBreakEnabled = label.timerProfile.isBreakEnabled
    val isLongBreakEnabled = label.timerProfile.isLongBreakEnabled

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
                            Text("Reset to default")
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
        AnimatedVisibility(!uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .verticalScroll(listState)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding(),
            ) {
                LabelNameRow(
                    isDefaultLabel = isDefaultLabel,
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
                ColorSelectRow(selectedIndex = label.colorIndex.toInt()) {
                    viewModel.setNewLabel(label.copy(colorIndex = it.toLong()))
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (!isDefaultLabel) {
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.setNewLabel(label.copy(useDefaultTimeProfile = !followDefault))
                        },
                        headlineContent = {
                            Text("Follow default time profile")
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
                                    title = "Focus time",
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
                                    title = "Break time",
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
                                        viewModel.setNewLabel(
                                            label.copy(
                                                timerProfile = label.timerProfile.copy(
                                                    isBreakEnabled = it,
                                                ),
                                            ),
                                        )
                                    },
                                )
                                EditableNumberListItem(
                                    title = "Long break time",
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
                                    title = "Sessions before long break",
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
                                            Text("Enable break budget")
                                            val tooltipState =
                                                rememberTooltipState(isPersistent = true)
                                            val coroutineScope = rememberCoroutineScope()
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                                                tooltip = {
                                                    PlainTooltip(
                                                        shape = MaterialTheme.shapes.small,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                                    ) {
                                                        Text(
                                                            modifier = Modifier.padding(8.dp),
                                                            text = "Your break budget increases according to your selected focus/break ratio and decreases when you're interrupted.\nTake breaks whenever you like.",
                                                        )
                                                    }
                                                },
                                                state = tooltipState,
                                            ) {
                                                IconButton(onClick = {
                                                    coroutineScope.launch {
                                                        tooltipState.show()
                                                    }
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Info,
                                                        contentDescription = "Enabled",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
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
                                    title = "Focus/break ratio",
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
        }
    }
}

@Composable
private fun LabelNameRow(
    isDefaultLabel: Boolean,
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
                    textDecoration = if (isDefaultLabel) null else TextDecoration.Underline,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                readOnly = isDefaultLabel,
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
                    text = if (isDefaultLabel) stringResource(R.string.label_default) else "Add label name",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(showError && labelName.isNotEmpty()) {
            Text(
                text = "Label name already exists",
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
private fun ColorSelectRow(selectedIndex: Int, onClick: (Int) -> Unit) {
    val colors = MaterialTheme.localColorsPalette.colors
    val listState = rememberLazyListState(selectedIndex)

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(colors) { index, color ->
            LabelColorPickerItem(
                color = color,
                isSelected = index == selectedIndex,
                onClick = {
                    onClick(index)
                },
            )
        }
    }
}

@Composable
private fun LabelColorPickerItem(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Color",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
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
                Text("Countdown")
            },
            selected = isCountDown,
        )

        FilterChip(
            onClick = { onCountDownEnabled(false) },
            label = {
                Text("Count-up")
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
        Text("Save")
    }
}
