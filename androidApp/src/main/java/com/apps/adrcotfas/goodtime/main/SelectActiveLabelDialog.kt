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
package com.apps.adrcotfas.goodtime.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import com.apps.adrcotfas.goodtime.data.model.isDefault
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.labels.main.unarchivedLabels
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.AlertDialogButtonStack
import com.apps.adrcotfas.goodtime.ui.common.SelectLabelDialog
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Edit
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectActiveLabelDialog(
    viewModel: LabelsViewModel = koinViewModel(),
    initialSelectedLabel: String,
    onNavigateToLabels: () -> Unit,
    onNavigateToActiveLabel: () -> Unit,
    onClearLabel: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val labels by viewModel.uiState
        .map { state ->
            state.unarchivedLabels.filter { !it.isDefault() }.map { it.getLabelData() }
        }.collectAsStateWithLifecycle(emptyList())

    val labelsIsEmpty = labels.isEmpty()
    val isDefaultLabelActive = initialSelectedLabel == Label.DEFAULT_LABEL_NAME

    SelectLabelDialog(
        title = stringResource(R.string.labels_select_active_label),
        singleSelection = true,
        labels = labels,
        initialSelectedLabels = listOf(initialSelectedLabel),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        extraContent = {
            if (labelsIsEmpty) EmptyState()
        },
        buttons = {
            AlertDialogButtonStack {
                FilledTonalButton(onClick = onNavigateToActiveLabel) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = EvaIcons.Outline.Edit,
                            contentDescription = null,
                        )
                        Text(
                            stringResource(
                                if (labelsIsEmpty || isDefaultLabelActive) {
                                    R.string.settings_timer_durations_title
                                } else {
                                    R.string.labels_edit_active_label
                                },
                            ),
                        )
                    }
                }
                TextButton(onClick = onNavigateToLabels) { Text(stringResource(R.string.labels_edit_labels)) }
                if (!isDefaultLabelActive) {
                    TextButton(onClick = onClearLabel) { Text(stringResource(R.string.labels_clear_label)) }
                }
            }
        },
    )
}

@Composable
private fun EmptyState() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.stats_no_items),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
    }
}
