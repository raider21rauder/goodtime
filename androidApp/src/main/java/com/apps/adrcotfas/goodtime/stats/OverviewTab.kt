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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun OverviewTab() {
    Text("Overview")
    // TODO:
    // First tab:
    // Weekly or daily goal with progress bar (e.g. 5 hours per day or 35 hours per week)

    // Overview - same as old version but with persistent time/sessions setting
    // History - same as old version but with minutes/ hours on the Y axis
    //
    // Permit selection of multiple labels and display as area chart

    // Productive time becomes "Time distribution"

    // Pie chart -> Same as before but use legend and "Others" for small slices
}
