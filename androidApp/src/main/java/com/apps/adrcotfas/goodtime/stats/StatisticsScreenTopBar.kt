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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.ui.common.BetterDropdownMenu
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.firstMenuItemModifier
import com.apps.adrcotfas.goodtime.ui.common.lastMenuItemModifier
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
    onSetShowBreaks: (Boolean) -> Unit,
    showBreaks: Boolean,
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
                            // TODO: consider plurals
                            Text("${if (selectionCount > 99) "99+" else selectionCount.toString()} items")
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
                        SelectLabelButton(selectedLabelsCount) {
                            onLabelButtonClick()
                        }
                        IconButton(onClick = {
                            showMore = true
                        }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                            BetterDropdownMenu(
                                expanded = showMore,
                                onDismissRequest = { showMore = false },
                            ) {
                                val paddingModifier = Modifier.padding(end = 32.dp)
                                DropdownMenuItem(
                                    modifier = firstMenuItemModifier,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add session",
                                        )
                                    },
                                    text = {
                                        Text(modifier = paddingModifier, text = "Add session")
                                    },
                                    onClick = {
                                        onAddButtonClick()
                                        showMore = false
                                    },
                                )
                                DropdownMenuItem(
                                    modifier = lastMenuItemModifier.toggleable(
                                        value = showBreaks,
                                        onValueChange = {
                                            onSetShowBreaks(!showBreaks)
                                            showMore = false
                                        },
                                    ),
                                    trailingIcon = {
                                        Checkbox(
                                            checked = showBreaks,
                                            onCheckedChange = null,
                                        )
                                    },
                                    text = {
                                        Text(modifier = paddingModifier, text = "Show breaks")
                                    },
                                    onClick = {
                                        onSetShowBreaks(!showBreaks)
                                        showMore = false
                                    },
                                )
                            }
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
