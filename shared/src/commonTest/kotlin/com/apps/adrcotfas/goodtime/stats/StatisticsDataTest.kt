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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class StatisticsDataTest {

    @Test
    fun calculateSessionWeightTest() {
        val todayTimestamp = 641131200000L

        // Test case: session from today
        val sessionToday = todayTimestamp
        assertEquals(1.0, calculateSessionWeight(sessionToday, todayTimestamp), 0.001)

        // Test case: session from 12 months ago
        val session12MonthsAgo = todayTimestamp - 12 * 30L * 24.hours.inWholeMilliseconds
        assertEquals(0.5, calculateSessionWeight(session12MonthsAgo, todayTimestamp), 0.001)

        // Test case: session from 24 months ago
        val session24MonthsAgo = todayTimestamp - 24 * 30L * 24.hours.inWholeMilliseconds
        assertEquals(0.0, calculateSessionWeight(session24MonthsAgo, todayTimestamp), 0.001)

        // Test case: session from 6 months ago
        val session6MonthsAgo = todayTimestamp - 6 * 30L * 24.hours.inWholeMilliseconds
        assertEquals(0.75, calculateSessionWeight(session6MonthsAgo, todayTimestamp), 0.001)

        // Test case: session from 25 months ago (should be clamped to 0)
        val session25MonthsAgo = todayTimestamp - 25 * 30L * 24.hours.inWholeMilliseconds
        assertEquals(0.0, calculateSessionWeight(session25MonthsAgo, todayTimestamp), 0.001)
    }
}
