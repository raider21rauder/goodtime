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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.adrcotfas.goodtime.common.Time
import com.apps.adrcotfas.goodtime.common.convertSpToDp
import com.apps.adrcotfas.goodtime.common.endOfWeekInThisWeek
import com.apps.adrcotfas.goodtime.common.entriesStartingWithThis
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInMonth
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInThisWeek
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.time.format.TextStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeatmapSection(
    firstDayOfWeek: DayOfWeek,
    data: HeatmapData,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val locale = remember { context.resources.configuration.locales[0] }

    val endLocalDate = remember { Time.currentDateTime().date }
    val startLocalDate = remember { endLocalDate.minus(DatePeriod(years = 1)) }

    val startAtStartOfWeek = remember { startLocalDate.firstDayOfWeekInThisWeek(firstDayOfWeek) }
    val endAtEndOfWeek = remember { endLocalDate.endOfWeekInThisWeek(firstDayOfWeek) }
    val numberOfWeeks = remember { startAtStartOfWeek.daysUntil(endAtEndOfWeek) / 7 }

    val cellSize = remember { (convertSpToDp(density, 11.sp.value) * 1.5f).dp }
    val cellSpacing = remember { cellSize / 6f }
    val daysInOrder = remember { firstDayOfWeek.entriesStartingWithThis() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = numberOfWeeks - 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Heatmap",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = color,
                ),
            )
        }

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 16.dp, horizontal = cellSize),
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(horizontal = cellSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(cellSize),
                    )
                    daysInOrder.forEach {
                        if (it.value % 2 == 1) {
                            Text(
                                modifier = Modifier.padding(cellSpacing).height(cellSize),
                                text = it.getDisplayName(TextStyle.SHORT, locale),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(cellSpacing)
                                    .size(cellSize),
                            )
                        }
                    }
                }
                Column {
                    LazyRow(
                        modifier = Modifier.wrapContentHeight(),
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
                    ) {
                        items(numberOfWeeks) { index ->
                            Column(
                                modifier = Modifier
                                    .wrapContentHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val currentWeekStart =
                                    startAtStartOfWeek.plus(DatePeriod(days = index * 7))

                                val monthName = currentWeekStart.month.getDisplayName(
                                    TextStyle.SHORT,
                                    locale,
                                )
                                if (currentWeekStart == startAtStartOfWeek || currentWeekStart == currentWeekStart.firstDayOfWeekInMonth(
                                        firstDayOfWeek,
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier.height(cellSize),
                                        text = monthName,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(cellSize))
                                }

                                daysInOrder.forEach { dayOfWeek ->
                                    val currentDay =
                                        currentWeekStart.plus(DatePeriod(days = dayOfWeek.value - 1))
                                    if (currentDay in startLocalDate..endLocalDate) {
                                        Box(modifier = Modifier.padding(cellSpacing)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(cellSize)
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer.copy(
                                                            alpha = 0.25f,
                                                        ),
                                                    ),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(cellSize)
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                                    .background(
                                                        color.copy(
                                                            alpha = data[currentDay]?.plus(0.2f) ?: 0f,
                                                        ),
                                                    ),
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .padding(cellSpacing)
                                                .size(cellSize),
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.size(cellSize / 2)) }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HeatmapSectionPreview() {
    HeatmapSection(
        firstDayOfWeek = DayOfWeek.MONDAY,
        data = emptyMap(),
    )
}
