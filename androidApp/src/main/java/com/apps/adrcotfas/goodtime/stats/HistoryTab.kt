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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatToPrettyDateAndTime
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.ui.common.enabledColors
import com.apps.adrcotfas.goodtime.ui.common.selectedColors

@Composable
fun HistoryTab(
    modifier: Modifier,
    sessions: LazyPagingItems<Session>,
    isSelectAllEnabled: Boolean,
    selectedSessions: List<Long>,
    unselectedSessions: List<Long>,
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
                val isSelected = selectedSessions.contains(session.id) ||
                    isSelectAllEnabled && !unselectedSessions.contains(session.id)
                HistoryListItem(
                    session = session,
                    colorIndex = labels.first { it.name == session.label }.colorIndex,
                    isSelected = isSelected,
                    is24HourFormat = is24HourFormat,
                    onClick = { onClick(session) },
                    onLongClick = { onLongClick(session) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListItem(
    session: Session,
    isSelected: Boolean = false,
    colorIndex: Long,
    is24HourFormat: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        colors = if (isSelected) ListItemDefaults.selectedColors() else ListItemDefaults.enabledColors(),
        leadingContent = {
            Row(modifier = Modifier.width(32.dp), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = session.duration.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                )
            }
        },
        headlineContent = {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val (date, time) = session.timestamp.formatToPrettyDateAndTime(is24HourFormat)
                Text(
                    text = "$date $time",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (session.interruptions > 0) {
                    Text(
                        text = "Interruptions: ${session.interruptions}",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                if (session.notes.isNotEmpty()) {
                    Text(
                        text = session.notes,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall.copy(
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                        ),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        trailingContent = {
            Row(modifier = Modifier.widthIn(max = 100.dp)) {
                if (session.label != Label.DEFAULT_LABEL_NAME) {
                    SmallLabelChip(name = session.label, colorIndex = colorIndex)
                }
            }
        },
    )
}

@Preview
@Composable
fun HistoryListItemPreview() {
    MaterialTheme {
        HistoryListItem(
            session = Session.default().copy(
                duration = 25,
                timestamp = System.currentTimeMillis(),
                label = "mathematics",
                interruptions = 12,
                notes = "Today was a good day and I did a lot of work and I am very happy",
            ),
            isSelected = false,
            colorIndex = 0,
            is24HourFormat = true,
            onClick = {},
            onLongClick = {},
        )
    }
}
