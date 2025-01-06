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

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatToPrettyDateAndTime
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session

@Composable
fun HistoryTab(
    modifier: Modifier,
    sessions: LazyPagingItems<Session>,
    labels: List<Label>,
    onClick: (Session) -> Unit,
    onLongClick: (Session) -> Unit,
) {
    val context = LocalContext.current
    val is24HourFormat = DateFormat.is24HourFormat(context)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(height = Dp.Infinity),
    ) {
        items(
            count = sessions.itemCount,
            key = sessions.itemKey { it.id },
            contentType = sessions.itemContentType { "sessions" },
        ) { index ->
            val session = sessions[index]
            if (session != null) {
                HistoryItem(
                    session,
                    labels.first { it.name == session.label }.colorIndex,
                    is24HourFormat,
                    { onClick(session) },
                    { onLongClick(session) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    session: Session,
    colorIndex: Long,
    is24HourFormat: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(0.7f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(0.25f),
                text = session.duration.toString(),
                style = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center),
            )
            val (date, time) = session.timestamp.formatToPrettyDateAndTime(is24HourFormat)
            Text(
                modifier = Modifier.weight(0.75f),
                text = "$date $time",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Row(modifier = Modifier.weight(0.3f), horizontalArrangement = Arrangement.End) {
            if (session.label != Label.DEFAULT_LABEL_NAME) {
                SmallLabelChip(name = session.label, colorIndex = colorIndex)
            }
        }
    }
}
