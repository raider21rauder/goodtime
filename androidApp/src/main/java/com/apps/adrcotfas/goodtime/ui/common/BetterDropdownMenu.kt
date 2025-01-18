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
package com.apps.adrcotfas.goodtime.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun BetterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        modifier = Modifier
            .crop(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp)),
        shape = MaterialTheme.shapes.medium,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        content()
    }
}

@Composable
fun BetterDropdownMenu(
    expanded: Boolean,
    value: String,
    onDismissRequest: () -> Unit,
    dropdownMenuOptions: List<String>,
    onDropdownMenuItemSelected: (Int) -> Unit,
) {
    BetterDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        dropdownMenuOptions.forEachIndexed { index, it ->
            DropdownMenuItem(
                modifier = if (it == value) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.1f,
                        ),
                    )
                } else {
                    Modifier
                },
                text = {
                    Text(
                        text = it,
                        style = if (it == value) {
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    )
                },
                onClick = {
                    onDropdownMenuItemSelected(index)
                    onDismissRequest()
                },
            )
        }
    }
}
