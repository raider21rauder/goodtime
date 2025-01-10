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
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.toExternal
import com.apps.adrcotfas.goodtime.data.model.toLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class LocalDataRepositoryImpl(
    private var sessionDao: SessionDao,
    private var labelDao: LabelDao,
    private val coroutineScope: CoroutineScope,
) : LocalDataRepository {

    init {
        insertDefaultLabel()
    }

    override fun reinitDatabase(database: ProductivityDatabase) {
        sessionDao = database.sessionsDao()
        labelDao = database.labelsDao()
        insertDefaultLabel()
    }

    private fun insertDefaultLabel() {
        coroutineScope.launch {
            val localLabel = Label.defaultLabel().toLocal()
            labelDao.insert(localLabel)
        }
    }

    override suspend fun insertSession(session: Session): Long {
        return sessionDao.insert(session.toLocal())
    }

    override suspend fun updateSession(id: Long, newSession: Session) {
        val localSession = newSession.toLocal()
        sessionDao.update(
            newTimestamp = localSession.timestamp,
            newDuration = localSession.duration,
            newInterruptions = localSession.interruptions,
            newLabel = localSession.labelName,
            newNotes = localSession.notes,
            id = id,
        )
    }

    override suspend fun updateSessionsLabelByIds(newLabel: String, ids: List<Long>) {
        sessionDao.updateLabelByIds(newLabel, ids)
    }

    override suspend fun updateSessionsLabelByIdsExcept(newLabel: String, unselectedIds: List<Long>, selectedLabels: List<String>) {
        sessionDao.updateLabelByIdsExcept(newLabel, unselectedIds, selectedLabels)
    }

    override fun selectAllSessions(): Flow<List<Session>> {
        return sessionDao.selectAll().map { it.map { sessions -> sessions.toExternal() } }
    }

    override fun selectSessionsAfter(timestamp: Long): Flow<List<Session>> {
        return sessionDao.selectAfter(timestamp)
            .map { sessions -> sessions.map { it.toExternal() } }
    }

    override fun selectSessionById(id: Long): Flow<Session> {
        return sessionDao.selectById(id).map { it.toExternal() }
    }

    override fun selectSessionsByIsArchived(isArchived: Boolean): Flow<List<Session>> {
        return sessionDao.selectByIsArchived(isArchived)
            .map { sessions -> sessions.map { it.toExternal() } }
    }

    override fun selectSessionsByLabel(label: String): Flow<List<Session>> {
        return sessionDao.selectByLabel(label).map { sessions -> sessions.map { it.toExternal() } }
    }

    override fun selectSessionsByLabels(labels: List<String>): Flow<List<Session>> {
        return sessionDao.selectByLabels(labels)
            .map { sessions -> sessions.map { it.toExternal() } }
    }

    override fun selectSessionsForHistoryPaged(labels: List<String>): PagingSource<Int, LocalSession> {
        return sessionDao.selectSessionsForHistoryPaged(labels)
    }

    override suspend fun deleteSessions(ids: List<Long>) {
        sessionDao.delete(ids)
    }

    override suspend fun deleteSessionsExcept(unselectedIds: List<Long>, selectedLabels: List<String>) {
        sessionDao.deleteExcept(unselectedIds, selectedLabels)
    }

    override suspend fun deleteAllSessions() {
        sessionDao.deleteAll()
    }

    override suspend fun insertLabel(label: Label): Long {
        return labelDao.insert(label.toLocal())
    }

    override suspend fun insertLabelAndBulkRearrange(
        label: Label,
        labelsToUpdate: List<Pair<String, Long>>,
    ) {
        labelDao.insertLabelAndBulkRearrange(label.toLocal(), labelsToUpdate)
    }

    override suspend fun updateLabelOrderIndex(name: String, newOrderIndex: Long) {
        labelDao.updateOrderIndex(newOrderIndex.toInt(), name)
    }

    override suspend fun bulkUpdateLabelOrderIndex(labelsToUpdate: List<Pair<String, Long>>) {
        labelDao.bulkUpdateLabelOrderIndex(labelsToUpdate)
    }

    override suspend fun updateLabel(
        name: String,
        newLabel: Label,
    ) {
        if (newLabel.name.isEmpty()) return
        val localLabel = newLabel.toLocal()
        labelDao.updateLabel(
            newName = localLabel.name,
            newColorIndex = localLabel.colorIndex,
            newUseDefaultTimeProfile = localLabel.useDefaultTimeProfile,
            newIsCountdown = localLabel.isCountdown,
            newWorkDuration = localLabel.workDuration,
            newIsBreakEnabled = localLabel.isBreakEnabled,
            newBreakDuration = localLabel.breakDuration,
            newIsLongBreakEnabled = localLabel.isLongBreakEnabled,
            newLongBreakDuration = localLabel.longBreakDuration,
            newSessionsBeforeLongBreak = localLabel.sessionsBeforeLongBreak,
            newWorkBreakRatio = localLabel.workBreakRatio,
            name = name,

        )
    }

    override suspend fun updateDefaultLabel(newDefaultLabel: Label) {
        updateLabel(Label.DEFAULT_LABEL_NAME, newDefaultLabel)
    }

    override fun selectDefaultLabel() = selectLabelByName(Label.DEFAULT_LABEL_NAME)

    override suspend fun updateLabelIsArchived(name: String, newIsArchived: Boolean) {
        labelDao.updateIsArchived(newIsArchived, name)
    }

    override fun selectLabelByName(name: String): Flow<Label?> {
        return labelDao.selectByName(name).map { it?.toExternal() }
    }

    override fun selectAllLabels(): Flow<List<Label>> {
        return labelDao.selectAll().map { labels -> labels.map { it.toExternal() } }
    }

    override fun selectLabelsByArchived(isArchived: Boolean): Flow<List<Label>> {
        return labelDao.selectByArchived(isArchived)
            .map { labels -> labels.map { it.toExternal() } }
    }

    override suspend fun deleteLabel(name: String) {
        labelDao.deleteByName(name)
    }

    override suspend fun deleteAllLabels() {
        labelDao.deleteAll()
    }
}
