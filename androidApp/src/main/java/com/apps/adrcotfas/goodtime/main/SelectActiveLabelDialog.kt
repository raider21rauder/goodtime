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

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import com.apps.adrcotfas.goodtime.data.model.isDefault
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.labels.main.unarchivedLabels
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.SelectLabelDialog
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectActiveLabelDialog(
    viewModel: LabelsViewModel = koinViewModel(),
    initialSelectedLabel: String,
    onNavigateToLabels: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val labels by viewModel.uiState
        .map { state -> state.unarchivedLabels.filter { !it.isDefault() }.map { it.getLabelData() } }
        .collectAsStateWithLifecycle(emptyList())

    SelectLabelDialog(
        title = stringResource(R.string.labels_select_active_label),
        confirmOnFirstPicked = false,
        multiSelect = false,
        labels = labels,
        initialSelectedLabels = listOf(initialSelectedLabel),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        neutralButton = {
            TextButton(onClick = onNavigateToLabels) { Text(stringResource(R.string.labels_edit_labels)) }
        },
    )
}
