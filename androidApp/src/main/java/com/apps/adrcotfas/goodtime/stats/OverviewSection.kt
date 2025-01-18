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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.local.SessionOverviewData
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import kotlin.time.Duration.Companion.minutes

@Composable
fun OverviewSection(
    data: SessionOverviewData,
    typeNames: Map<OverviewDurationType, String>,
    showBreak: Boolean,
) {
    val color = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Overview",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = color,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            OverviewDurationType.entries.forEach { overviewType ->
                val valueWork = when (overviewType) {
                    OverviewDurationType.TODAY -> data.workToday
                    OverviewDurationType.THIS_WEEK -> data.workThisWeek
                    OverviewDurationType.THIS_MONTH -> data.workThisMonth
                    OverviewDurationType.TOTAL -> data.workTotal
                }.minutes.formatOverview()
                val valueBreak = when (overviewType) {
                    OverviewDurationType.TODAY -> data.breakToday
                    OverviewDurationType.THIS_WEEK -> data.breakThisWeek
                    OverviewDurationType.THIS_MONTH -> data.breakThisMonth
                    OverviewDurationType.TOTAL -> data.breakTotal
                }.minutes.formatOverview()

                OverviewTypeSection(
                    modifier = Modifier.weight(1f / OverviewDurationType.entries.size),
                    title = typeNames[overviewType]!!,
                    valueWork = valueWork,
                    colorWork = color,
                    showBreak = showBreak,
                    valueBreak = valueBreak,
                    colorBreak = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun OverviewTypeSection(
    modifier: Modifier = Modifier,
    title: String,
    valueWork: String,
    colorWork: Color,
    showBreak: Boolean,
    valueBreak: String,
    colorBreak: Color,
) {
    Column(
        modifier = modifier.animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            valueWork,
            style = MaterialTheme.typography.labelLarge.copy(
                color = colorWork,
                textAlign = TextAlign.Center,
            ),
        )
        if (showBreak) {
            Text(
                valueBreak,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = colorBreak,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
