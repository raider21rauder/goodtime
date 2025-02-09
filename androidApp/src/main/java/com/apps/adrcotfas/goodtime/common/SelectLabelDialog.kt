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
package com.apps.adrcotfas.goodtime.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.apps.adrcotfas.goodtime.stats.LabelChip
import com.apps.adrcotfas.goodtime.stats.LabelData
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Edit

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SelectLabelDialog(
    title: String,
    singleSelection: Boolean,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    labels: List<LabelData>,
    initialSelectedLabels: List<String> = emptyList(),
    extraButtonText: String? = null,
    onExtraButtonClick: (() -> Unit)? = null,
) {
    val selectedLabels = rememberMutableStateListOf(*initialSelectedLabels.toTypedArray())

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .background(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = 24.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    labels.forEach { label ->
                        LabelChip(
                            label.name,
                            label.colorIndex,
                            selected = selectedLabels.contains(label.name),
                            showIcon = !singleSelection,
                        ) {
                            if (singleSelection) {
                                onConfirm(listOf(label.name))
                            } else {
                                val alreadySelected = selectedLabels.contains(label.name)
                                if (selectedLabels.size == 1 && alreadySelected) return@LabelChip
                                if (selectedLabels.contains(label.name)) {
                                    selectedLabels.remove(label.name)
                                } else {
                                    selectedLabels.add(label.name)
                                }
                            }
                        }
                    }
                }
                val hasExtraButton = onExtraButtonClick != null && extraButtonText != null
                if (!singleSelection || hasExtraButton) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (hasExtraButton) {
                            FilledTonalButton(onClick = onExtraButtonClick!!) {
                                Icon(
                                    imageVector = EvaIcons.Outline.Edit,
                                    contentDescription = extraButtonText,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(extraButtonText!!)
                            }
                        } else {
                            TextButton(onClick = onDismiss) { Text(stringResource(id = android.R.string.cancel)) }
                            TextButton(onClick = { onConfirm(selectedLabels) }) {
                                Text(
                                    stringResource(id = android.R.string.ok),
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
