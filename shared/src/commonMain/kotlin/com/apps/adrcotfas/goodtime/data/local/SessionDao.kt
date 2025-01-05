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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LocalSession): Long

    @Query(
        """
        UPDATE localSession
        SET timestamp = :newTimestamp, duration = :newDuration, interruptions = :newInterruptions, labelName = :newLabel, notes = :newNotes
        WHERE id = :id
    """,
    )
    suspend fun update(
        newTimestamp: Long,
        newDuration: Long,
        newInterruptions: Long,
        newLabel: String,
        newNotes: String,
        id: Long,
    )

    @Query("UPDATE localSession SET labelName = :newLabel WHERE id IN (:ids)")
    suspend fun updateLabelByIds(newLabel: String, ids: List<Long>)

    @Query("SELECT * FROM localSession ORDER BY timestamp DESC")
    fun selectAll(): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    fun selectAfter(timestamp: Long): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE id = :id")
    fun selectById(id: Long): Flow<LocalSession>

    @Query("SELECT * FROM localSession WHERE isArchived = :isArchived ORDER BY timestamp DESC")
    fun selectByIsArchived(isArchived: Boolean): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE labelName = :labelName ORDER BY timestamp DESC")
    fun selectByLabel(labelName: String): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE labelName IN (:labelNames) ORDER BY timestamp DESC")
    fun selectByLabels(labelNames: List<String>): Flow<List<LocalSession>>

    @Query("DELETE FROM localSession WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM localSession WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM localSession")
    suspend fun deleteAll()

    @RawQuery
    fun checkpoint(query: RoomRawQuery = RoomRawQuery("PRAGMA wal_checkpoint(FULL)")): Int
}
