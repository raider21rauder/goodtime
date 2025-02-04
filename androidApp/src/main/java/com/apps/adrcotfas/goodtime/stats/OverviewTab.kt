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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apps.adrcotfas.goodtime.common.Time.currentDateTime
import com.apps.adrcotfas.goodtime.common.isoWeekNumber
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.data.settings.StatisticsSettings
import com.apps.adrcotfas.goodtime.stats.history.HistorySection
import kotlinx.datetime.DayOfWeek
import java.time.format.TextStyle

@Composable
fun OverviewTab(
    firstDayOfWeek: DayOfWeek,
    statisticsSettings: StatisticsSettings,
    statisticsData: StatisticsData,
    onChangeOverviewType: (OverviewType) -> Unit,
    onChangeOverviewDurationType: (OverviewDurationType) -> Unit,
    historyChartViewModel: StatisticsHistoryViewModel,
) {
    val context = LocalContext.current
    val locale = remember { context.resources.configuration.locales[0] }
    val currentDateTime = remember { currentDateTime() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        val typeNames = mapOf(
            OverviewDurationType.TODAY to "Today", // TODO: extract to strings
            OverviewDurationType.THIS_WEEK to "Week ${currentDateTime.date.isoWeekNumber()}",
            OverviewDurationType.THIS_MONTH to currentDateTime.month.getDisplayName(
                TextStyle.FULL,
                locale,
            ),
            OverviewDurationType.TOTAL to "Total",
        )

        OverviewSection(
            statisticsData.overviewData,
            typeNames,
            statisticsSettings.overviewType,
            onChangeOverviewType,
        )

        HistorySection(historyChartViewModel)

        HeatmapSection(
            firstDayOfWeek,
            data = statisticsData.heatmapData,
        )

        WorkBreakRatioSection(
            statisticsData.overviewData,
            statisticsSettings.overviewDurationType,
            onChangeOverviewDurationType,
            typeNames = typeNames,
        )

        PieChartSection(
            statisticsData.overviewData,
            statisticsSettings.overviewDurationType,
            onChangeOverviewDurationType,
            typeNames = typeNames,
            selectedLabels = historyChartViewModel.uiState.value.selectedLabels,
        )
    }

    // TODO:
    // Weekly or daily goal with progress bar (e.g. 5 hours per day or 35 hours per week)

    // History - same as old version but with minutes/ hours on the Y axis
    // Permit selection of multiple labels and display as area chart
    // Pie chart -> Same as before but use legend and "Others" for small slices
}
