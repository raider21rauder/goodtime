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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Trash

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreenTopBar(
    onAddButtonClick: () -> Unit,
    onLabelButtonClick: () -> Unit,
    selectedLabelsCount: Int,
    onCancel: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAll: () -> Unit,
    showSelectionUi: Boolean,
    selectionCount: Int,
    showSeparator: Boolean,
) {
    val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = Color.Transparent,
    )

    var showMore by rememberSaveable { mutableStateOf(false) }
    Column {
        Crossfade(showSelectionUi, label = "StatsScreen TopBar") {
            if (it) {
                TopAppBar(
                    title = {
                        if (selectionCount != 0) {
                            Text(if (selectionCount > 99) "99+" else selectionCount.toString())
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onDeleteClick()
                        }) {
                            Icon(
                                imageVector = EvaIcons.Outline.Trash,
                                contentDescription = "Delete",
                            )
                        }
                        IconButton(onClick = onSelectAll) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all",
                            )
                        }
                        IconButton(onClick = onLabelButtonClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Label,
                                contentDescription = "Select labels",
                            )
                        }
                    },
                    navigationIcon = {
                        if (showSelectionUi) {
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Cancel",
                                )
                            }
                        }
                    },
                    colors = colors,
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text("Statistics")
                    },
                    actions = {
                        IconButton(onClick = {
                            onAddButtonClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add session",
                            )
                        }
                        SelectLabelButton(selectedLabelsCount) {
                            onLabelButtonClick()
                        }
                    },
                    colors = colors,
                )
            }
        }
        if (showSeparator) {
            SubtleHorizontalDivider()
        }
    }
}
