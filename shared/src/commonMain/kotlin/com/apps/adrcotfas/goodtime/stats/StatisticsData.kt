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

import com.apps.adrcotfas.goodtime.common.Time
import com.apps.adrcotfas.goodtime.common.Time.toLocalDateTime
import com.apps.adrcotfas.goodtime.common.toEpochMilliseconds
import com.apps.adrcotfas.goodtime.data.model.Session
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class SessionOverviewData(
    val workTodayPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsToday: Long = 0,
    val workToday: Long = 0,
    val breakToday: Long = 0,
    val workThisWeekPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsThisWeek: Long = 0,
    val workThisWeek: Long = 0,
    val breakThisWeek: Long = 0,
    val workThisMonthPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsThisMonth: Long = 0,
    val workThisMonth: Long = 0,
    val breakThisMonth: Long = 0,
    val workTotalPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsTotal: Long = 0,
    val workTotal: Long = 0,
    val breakTotal: Long = 0,
)

typealias HeatmapData = Map<LocalDate, Float>

typealias ProductiveHoursOfTheDay = Map<Int, Float>

data class StatisticsData(
    val overviewData: SessionOverviewData = SessionOverviewData(),
    val heatmapData: HeatmapData = emptyMap(),
    val productiveHoursOfTheDay: ProductiveHoursOfTheDay = emptyMap(),
)

fun computeStatisticsData(
    sessions: List<Session>,
    firstDayOfWeek: DayOfWeek,
    workDayStart: Int,
): StatisticsData {
    val today = Time.startOfTodayMillis()
    val startOfThisWeekDate = Time.startOfThisWeekAdjusted(firstDayOfWeek)
    val startOfThisWeek = startOfThisWeekDate.toEpochMilliseconds()
    val startOfThisMonthDate = Time.startOfThisMonth()
    val startOfThisMonth = startOfThisMonthDate.toEpochMilliseconds()

    val workTodayPerLabel = mutableMapOf<String, Long>()
    var workSessionsToday = 0L
    var workToday = 0L
    var breakToday = 0L

    val workThisWeekPerLabel = mutableMapOf<String, Long>()
    var workSessionsThisWeek = 0L
    var workThisWeek = 0L
    var breakThisWeek = 0L

    val workThisMonthPerLabel = mutableMapOf<String, Long>()
    var workSessionsThisMonth = 0L
    var workThisMonth = 0L
    var breakThisMonth = 0L

    val workTotalPerLabel = mutableMapOf<String, Long>()
    var workSessionsTotal = 0L
    var workTotal = 0L
    var breakTotal = 0L

    val todayLocalDate = Time.currentDateTime().date
    val oneYearAgoLocalDate = todayLocalDate.minus(DatePeriod(years = 1))

    val heatmapData = mutableMapOf<LocalDate, Float>()
    var maxHeatMapValue = 1f

    val productiveHoursOfTheDay = mutableMapOf<Int, Float>()
    var maxProductiveHourOfTheDay = 1f

    val oneYearAgoMillis = oneYearAgoLocalDate.toEpochMilliseconds()

    sessions.asSequence().map {
        // Adjust the timestamp to the middle of the session for a more accurate heatmap
        val timestamp = it.timestamp - it.duration.minutes.inWholeMilliseconds / 2
        // Adjust the timestamp to the start of the work day
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

        if (session.isWork) {
            // TODO: extract productive hours computation to separate to function
            if (today - session.adjustedTimestamp < oneYearAgoMillis) {
                heatmapData[date] = (heatmapData[date] ?: 0f) + session.duration
                maxHeatMapValue = maxOf(maxHeatMapValue, heatmapData[date] ?: 0f)

                val weight = calculateSessionWeight(session.timestamp, today)
                productiveHoursOfTheDay[session.dateTime.hour] =
                    (
                        productiveHoursOfTheDay[session.adjustedDateTime.hour]
                            ?: 0f
                        ) + session.duration * weight
                maxProductiveHourOfTheDay = maxOf(
                    maxProductiveHourOfTheDay,
                    productiveHoursOfTheDay[session.dateTime.hour] ?: 0f,
                )
            }

            if (session.adjustedTimestamp >= today) {
                workToday += session.duration
                workTodayPerLabel[session.label] =
                    (workTodayPerLabel[session.label] ?: 0L) + session.duration
                workSessionsToday++
            }
            // TODO: consider end of this week
            if (session.adjustedTimestamp >= startOfThisWeek) {
                workThisWeek += session.duration
                workThisWeekPerLabel[session.label] =
                    (workThisWeekPerLabel[session.label] ?: 0L) + session.duration
                workSessionsThisWeek++
            }
            // TODO: consider end of this month
            if (session.adjustedTimestamp >= startOfThisMonth) {
                workThisMonth += session.duration
                workThisMonthPerLabel[session.label] =
                    (workThisMonthPerLabel[session.label] ?: 0L) + session.duration
                workSessionsThisMonth++
            }
            workTotal += session.duration
            workTotalPerLabel[session.label] =
                (workTotalPerLabel[session.label] ?: 0L) + session.duration
            workSessionsTotal++
        } else {
            if (session.adjustedTimestamp >= today) {
                breakToday += session.duration
            }
            if (session.adjustedTimestamp >= startOfThisWeek) {
                breakThisWeek += session.duration
            }
            if (session.adjustedTimestamp >= startOfThisMonth) {
                breakThisMonth += session.duration
            }
            breakTotal += session.duration
        }
    }

    // normalize the values for the heatmap
    heatmapData.forEach {
        heatmapData[it.key] = (it.value / maxHeatMapValue).coerceIn(0f, 1f)
    }

    productiveHoursOfTheDay.forEach {
        productiveHoursOfTheDay[it.key] = (it.value / maxProductiveHourOfTheDay).coerceIn(0f, 1f)
    }

    val overviewData = SessionOverviewData(
        workTodayPerLabel = workTodayPerLabel,
        workSessionsToday = workSessionsToday,
        workToday = workToday,
        breakToday = breakToday,
        workThisWeekPerLabel = workThisWeekPerLabel,
        workSessionsThisWeek = workSessionsThisWeek,
        workThisWeek = workThisWeek,
        breakThisWeek = breakThisWeek,
        workThisMonthPerLabel = workThisMonthPerLabel,
        workSessionsThisMonth = workSessionsThisMonth,
        workThisMonth = workThisMonth,
        breakThisMonth = breakThisMonth,
        workTotalPerLabel = workTotalPerLabel,
        workSessionsTotal = workSessionsTotal,
        workTotal = workTotal,
        breakTotal = breakTotal,
    )

    return StatisticsData(
        overviewData = overviewData,
        heatmapData = heatmapData,
        productiveHoursOfTheDay = productiveHoursOfTheDay,
    )
}

/**
 * The older the session, the less weight it has.
 * For a session from 365 days ago, the weight is 0. For a session from today, the weight is 1.
 */
fun calculateSessionWeight(sessionTimestamp: Long, todayTimestamp: Long): Float {
    val daysDifference = (todayTimestamp - sessionTimestamp).milliseconds.inWholeDays
    return (365 - daysDifference).coerceIn(0L, 365L) / 365f
}
