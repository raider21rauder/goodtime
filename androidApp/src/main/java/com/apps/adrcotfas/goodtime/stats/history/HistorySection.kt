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
package com.apps.adrcotfas.goodtime.stats.history

import android.text.Layout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.prettyName
import com.apps.adrcotfas.goodtime.common.prettyNames
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.apps.adrcotfas.goodtime.stats.StatisticsHistoryViewModel
import com.apps.adrcotfas.goodtime.ui.common.DropdownMenuBox
import com.apps.adrcotfas.goodtime.ui.localColorsPalette
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.cartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.stacked
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import java.text.DecimalFormat
import java.time.format.TextStyle

@Composable
fun HistorySection(viewModel: StatisticsHistoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.data.x.isEmpty()) return
    val data = uiState.data
    val type = uiState.type

    val primaryColor = MaterialTheme.colorScheme.primary

    val colors =
        uiState.selectedLabels.map {
            if (it.name == Label.DEFAULT_LABEL_NAME) {
                primaryColor
            } else {
                MaterialTheme.localColorsPalette.colors[it.colorIndex.toInt()]
            }
        }.plus(MaterialTheme.localColorsPalette.colors[Label.OTHERS_LABEL_COLOR_INDEX])

    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]

    val x = remember(data) { data.x }
    val y = remember(data) { data.y }

    val modelProducer = remember { CartesianChartModelProducer() }

    val bottomAxisStrings = remember(locale) {
        BottomAxisStrings(
            dayOfWeekNames = DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, locale) },
            monthsOfYearNames = Month.entries.map { it.getDisplayName(TextStyle.SHORT, locale) },
        )
    }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries { y.values.forEach { series(it) } }
            extras { it[timestampsKey] = x }
            extras { it[labelsKey] = y.keys }
            extras { it[extraBottomAxisStrings] = bottomAxisStrings }
            extras { it[extraViewType] = type }
            extras { it[extraFirstDayOfWeek] = uiState.firstDayOfWeek }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "History",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = primaryColor,
                ),
            )

            DropdownMenuBox(
                value = type.prettyName(),
                options = prettyNames<HistoryIntervalType>(),
                onDismissRequest = {},
                onDropdownMenuItemSelected = {
                    viewModel.setType(HistoryIntervalType.entries[it])
                },
            )
        }

        AggregatedHistoryChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            modelProducer = modelProducer,
            colors = colors,
        )
    }
}

private val yDecimalFormat = DecimalFormat("#.# h")
private val startAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
    yDecimalFormat.format(value / 60)
}
private val startAxisItemPlacer = VerticalAxis.ItemPlacer.step({ 30.0 })

@Composable
private fun AggregatedHistoryChart(
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer,
    colors: List<Color>,
) {
    val defaultLabelName = stringResource(id = R.string.label_default)
    val othersLabelName = stringResource(id = R.string.others)
    val totalLabel = stringResource(id = R.string.total)
    val othersLabelColor = colors.last().toArgb()

    val scrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = Scroll.Absolute.End,
        autoScrollCondition = AutoScrollCondition.OnModelGrowth,
    )

    val markerValueFormatter = HistoryChartMarkerValueFormatter(
        defaultLabelName = defaultLabelName,
        othersLabelName = othersLabelName,
        othersLabelColor = othersLabelColor,
        totalLabel = totalLabel,
    )

    ProvideVicoTheme(
        rememberM3VicoTheme(
            lineColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.25f,
            ),
        ),
    ) {
        CartesianChartHost(
            modifier = modifier.height(200.dp),
            scrollState = scrollState,
            zoomState = rememberVicoZoomState(zoomEnabled = false),
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider =
                    ColumnCartesianLayer.ColumnProvider.series(
                        colors.map { color ->
                            rememberLineComponent(fill = fill(color), thickness = 18.dp)
                        },
                    ),
                    columnCollectionSpacing = 18.dp,
                    mergeMode = { ColumnCartesianLayer.MergeMode.stacked() },
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = startAxisValueFormatter,
                    itemPlacer = startAxisItemPlacer,
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    guideline = null,
                    label = rememberAxisLabelComponent(
                        lineCount = 2,
                        textAlignment = Layout.Alignment.ALIGN_CENTER,
                    ),
                    valueFormatter = BottomAxisValueFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                ),
                marker = rememberMarker(markerValueFormatter),
                layerPadding = { cartesianLayerPadding(scalableStart = 8.dp, scalableEnd = 8.dp) },
            ),
            modelProducer = modelProducer,
        )
    }
}
