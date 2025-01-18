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
package com.apps.adrcotfas.goodtime.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import kotlinx.coroutines.flow.Flow

data class SessionOverviewData(
    val workToday: Long = 0,
    val breakToday: Long = 0,
    val workThisWeek: Long = 0,
    val breakThisWeek: Long = 0,
    val workThisMonth: Long = 0,
    val breakThisMonth: Long = 0,
    val workTotal: Long = 0,
    val breakTotal: Long = 0,
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LocalSession): Long

    @Query(
        """
        UPDATE localSession
        SET timestamp = :newTimestamp, duration = :newDuration, interruptions = :newInterruptions, labelName = :newLabel, notes = :newNotes, isWork = :newIsWork
        WHERE id = :id
    """,
    )
    suspend fun update(
        newTimestamp: Long,
        newDuration: Long,
        newInterruptions: Long,
        newLabel: String,
        newNotes: String,
        newIsWork: Boolean,
        id: Long,
    )

    @Query("UPDATE localSession SET labelName = :newLabel WHERE id IN (:ids)")
    suspend fun updateLabelByIds(newLabel: String, ids: List<Long>)

    @Query(
        """
        UPDATE localSession SET labelName = :newLabel
        WHERE isArchived = 0
        AND id NOT IN (:ids)
        AND labelName IN (:labels)
        AND (:considerBreaks = 1 OR isWork = 1)
        """,
    )
    suspend fun updateLabelByIdsExcept(
        newLabel: String,
        ids: List<Long>,
        labels: List<String>,
        considerBreaks: Boolean,
    )

    @Query("SELECT * FROM localSession ORDER BY timestamp DESC")
    fun selectAll(): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    fun selectAfter(timestamp: Long): Flow<List<LocalSession>>

    @Query(
        """
        SELECT
            -- Today
            SUM(CASE WHEN isWork = 1 AND labelName IN (:labels) AND timestamp >= :todayStart THEN duration ELSE 0 END) AS workToday,
            SUM(CASE WHEN isWork = 0 AND labelName IN (:labels) AND timestamp >= :todayStart THEN duration ELSE 0 END) AS breakToday,
            -- This Week
            SUM(CASE WHEN isWork = 1 AND labelName IN (:labels) AND timestamp >= :weekStart THEN duration ELSE 0 END) AS workThisWeek,
            SUM(CASE WHEN isWork = 0 AND labelName IN (:labels) AND timestamp >= :weekStart THEN duration ELSE 0 END) AS breakThisWeek,
            -- This Month
            SUM(CASE WHEN isWork = 1 AND labelName IN (:labels) AND timestamp >= :monthStart THEN duration ELSE 0 END) AS workThisMonth,
            SUM(CASE WHEN isWork = 0 AND labelName IN (:labels) AND timestamp >= :monthStart THEN duration ELSE 0 END) AS breakThisMonth,
            -- Total
            SUM(CASE WHEN isWork = 1 AND labelName IN (:labels) THEN duration ELSE 0 END) AS workTotal,
            SUM(CASE WHEN isWork = 0 AND labelName IN (:labels) THEN duration ELSE 0 END) AS breakTotal
        FROM localSession
        WHERE labelName IN (:labels)
        """,
    )
    fun selectOverviewAfter(
        todayStart: Long,
        weekStart: Long,
        monthStart: Long,
        labels: List<String>,
    ): Flow<SessionOverviewData>

    @Query("SELECT * FROM localSession WHERE id = :id")
    fun selectById(id: Long): Flow<LocalSession>

    @Query("SELECT * FROM localSession WHERE isArchived = :isArchived ORDER BY timestamp DESC")
    fun selectByIsArchived(isArchived: Boolean): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE labelName = :labelName ORDER BY timestamp DESC")
    fun selectByLabel(labelName: String): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE labelName IN (:labelNames) ORDER BY timestamp DESC")
    fun selectByLabels(labelNames: List<String>): Flow<List<LocalSession>>

    @Query(
        """
        SELECT * FROM localSession
        WHERE labelName IN (:labelNames)
        AND (:considerBreaks = 1 OR isWork = 1)
        ORDER BY timestamp DESC
    """,
    )
    fun selectSessionsForHistoryPaged(
        labelNames: List<String>,
        considerBreaks: Boolean,
    ): PagingSource<Int, LocalSession>

    @Query("DELETE FROM localSession WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query(
        """
        DELETE FROM localSession
        WHERE id NOT IN (:ids)
        AND labelName IN (:labels)
        AND (:considerBreaks = 1 OR isWork = 1)
        """,
    )
    suspend fun deleteExcept(ids: List<Long>, labels: List<String>, considerBreaks: Boolean)

    @Query("DELETE FROM localSession")
    suspend fun deleteAll()

    @RawQuery
    fun checkpoint(query: RoomRawQuery = RoomRawQuery("PRAGMA wal_checkpoint(FULL)")): Int
}
