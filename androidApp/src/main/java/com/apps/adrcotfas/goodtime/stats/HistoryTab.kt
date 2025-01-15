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
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
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
import com.apps.adrcotfas.goodtime.shared.R as SharedR

@Composable
fun HistoryTab(
    sessions: LazyPagingItems<Session>,
    isSelectAllEnabled: Boolean,
    selectedSessions: List<Long>,
    unselectedSessions: List<Long>,
    labels: List<Label>,
    onClick: (Session) -> Unit,
    onLongClick: (Session) -> Unit,
    listState: LazyListState,
) {
    val context = LocalContext.current
    val is24HourFormat = DateFormat.is24HourFormat(context)

    LaunchedEffect(sessions.itemCount) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect {
                listState.animateScrollToItem(0)
            }
    }

    if (sessions.itemCount == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No items",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = Dp.Infinity),
        state = listState,
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
                    modifier = Modifier.animateItem(),
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
    modifier: Modifier = Modifier,
    session: Session,
    isSelected: Boolean = false,
    colorIndex: Long,
    is24HourFormat: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        colors = if (isSelected) ListItemDefaults.selectedColors() else ListItemDefaults.enabledColors(),
        headlineContent = {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Image(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(
                            if (session.isWork) {
                                SharedR.drawable.ic_status_goodtime
                            } else {
                                SharedR.drawable.ic_break
                            },
                        ),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        contentDescription = "session type",
                    )

                    Spacer(modifier = Modifier.size(4.dp))

                    Text(
                        text = "${session.duration}min",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )

                    if (session.interruptions > 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Image(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(SharedR.drawable.ic_broken_link),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                            contentDescription = "interruptions",
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "${session.interruptions}min",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                }

                val (date, time) = session.timestamp.formatToPrettyDateAndTime(is24HourFormat)
                Text(
                    text = "$date $time",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                )
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
            Row(modifier = Modifier.widthIn(max = 125.dp)) {
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
