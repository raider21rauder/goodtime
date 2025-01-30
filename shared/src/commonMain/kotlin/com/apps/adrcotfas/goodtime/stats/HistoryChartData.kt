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

import com.apps.adrcotfas.goodtime.common.Time.currentDateTime
import com.apps.adrcotfas.goodtime.common.Time.toLocalDateTime
import com.apps.adrcotfas.goodtime.common.firstDayOfThisQuarter
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInThisWeek
import com.apps.adrcotfas.goodtime.common.toEpochMilliseconds
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_DAYS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_MONTHS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_QUARTERS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_WEEKS
import com.apps.adrcotfas.goodtime.stats.HistoryChartData.Companion.NUM_YEARS
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryChartData(
    val x: List<Long> = emptyList(),
    val y: Map<String, List<Long>> = emptyMap(),
) {
    companion object {
        const val NUM_DAYS = 365
        const val NUM_WEEKS = 105
        const val NUM_MONTHS = 24
        const val NUM_QUARTERS = 20
        const val NUM_YEARS = 10
    }
}

fun computeHistoryChartData(
    sessions: List<Session>,
    workDayStart: Int,
    firstDayOfWeek: DayOfWeek,
    type: HistoryIntervalType,
): HistoryChartData {
    val today = currentDateTime().date
    val reference = when (type) {
        HistoryIntervalType.DAYS -> {
            today
        }

        HistoryIntervalType.WEEKS -> {
            today.firstDayOfWeekInThisWeek(firstDayOfWeek)
        }

        HistoryIntervalType.MONTHS -> {
            LocalDate(today.year, today.month, 1)
        }

        HistoryIntervalType.QUARTERS -> {
            today.firstDayOfThisQuarter()
        }

        HistoryIntervalType.YEARS -> {
            LocalDate(today.year, 1, 1)
        }
    }

    val numDummyValues = when (type) {
        HistoryIntervalType.DAYS -> NUM_DAYS
        HistoryIntervalType.WEEKS -> NUM_WEEKS
        HistoryIntervalType.MONTHS -> NUM_MONTHS
        HistoryIntervalType.QUARTERS -> NUM_QUARTERS
        HistoryIntervalType.YEARS -> NUM_YEARS
    }

    val x = mutableListOf<LocalDate>()
    val y = mutableMapOf(Label.DEFAULT_LABEL_NAME to MutableList(numDummyValues) { 0L })

    var tmpDate = reference

    // Initialize the data with zeros
    when (type) {
        HistoryIntervalType.DAYS -> {
            repeat(numDummyValues) {
                x.add(tmpDate)
                tmpDate = tmpDate.minus(DatePeriod(days = 1))
            }
        }

        HistoryIntervalType.WEEKS -> {
            repeat(numDummyValues) {
                x.add(tmpDate)
                tmpDate = tmpDate.minus(DatePeriod(days = 7))
            }
        }

        HistoryIntervalType.MONTHS -> {
            repeat(numDummyValues) {
                x.add(tmpDate)
                tmpDate = tmpDate.minus(DatePeriod(months = 1))
            }
        }

        HistoryIntervalType.QUARTERS -> {
            repeat(numDummyValues) {
                x.add(tmpDate)
                tmpDate = tmpDate.minus(DatePeriod(months = 3))
            }
        }

        HistoryIntervalType.YEARS -> {
            repeat(numDummyValues) {
                x.add(tmpDate)
                tmpDate = tmpDate.minus(DatePeriod(years = 1))
            }
        }
    }

    x.reverse()

    val emptyList = List(numDummyValues) { 0L }
    sessions.asSequence().map {
        val timestamp = it.timestamp - it.duration.minutes.inWholeMilliseconds / 2
        val adjustedTimestamp = timestamp - workDayStart.seconds.inWholeMilliseconds
        PreProcessingSession(
            label = it.label,
            timestamp = timestamp,
            dateTime = toLocalDateTime(timestamp),
            adjustedTimestamp = adjustedTimestamp,
            adjustedDateTime = toLocalDateTime(adjustedTimestamp),
            duration = it.duration,
            isWork = it.isWork,
        )
    }.forEach { session ->
        val date = session.adjustedDateTime.date
        val label = session.label

        if (session.isWork) {
            when (type) {
                HistoryIntervalType.DAYS -> {
                    if (date in x) {
                        y[label] = (y[label] ?: emptyList.toMutableList()).apply {
                            this[x.indexOf(date)] += session.duration
                        }
                    }
                }

                HistoryIntervalType.WEEKS -> {
                    val startOfWeek = date.firstDayOfWeekInThisWeek(firstDayOfWeek)
                    if (startOfWeek in x) {
                        y[label] = (y[label] ?: emptyList.toMutableList()).apply {
                            this[x.indexOf(startOfWeek)] += session.duration
                        }
                    }
                }

                HistoryIntervalType.MONTHS -> {
                    val startOfMonth = LocalDate(date.year, date.month, 1)
                    if (startOfMonth in x) {
                        y[label] = (y[label] ?: emptyList.toMutableList()).apply {
                            this[x.indexOf(startOfMonth)] += session.duration
                        }
                    }
                }

                HistoryIntervalType.QUARTERS -> {
                    val startOfQuarter = date.firstDayOfThisQuarter()
                    if (startOfQuarter in x) {
                        y[label] = (y[label] ?: emptyList.toMutableList()).apply {
                            this[x.indexOf(startOfQuarter)] += session.duration
                        }
                    }
                }
                HistoryIntervalType.YEARS -> {
                    val startOfYear = LocalDate(date.year, 1, 1)
                    if (startOfYear in x) {
                        y[label] = (y[label] ?: emptyList.toMutableList()).apply {
                            this[x.indexOf(startOfYear)] += session.duration
                        }
                    }
                }
            }
        }
    }
    return HistoryChartData(x = x.map { it.toEpochMilliseconds() }, y = y)
}
