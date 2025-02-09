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
package com.apps.adrcotfas.goodtime.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.settings.NotificationPermissionState
import com.apps.adrcotfas.goodtime.ui.common.IconListItem
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Bell
import compose.icons.evaicons.outline.ColorPalette
import compose.icons.evaicons.outline.Settings
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToTimerStyle: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val notificationPermissionState by viewModel.uiState.map { it.settings.notificationPermissionState }
        .collectAsStateWithLifecycle(initialValue = NotificationPermissionState.NOT_ASKED)

    val listState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Settings",
                onNavigateBack = { onNavigateBack() },
                showSeparator = listState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(listState),
        ) {
            ActionSection(
                notificationPermissionState = notificationPermissionState,
                onNotificationPermissionGranted = { granted ->
                    viewModel.setNotificationPermissionGranted(granted)
                },
            )
            IconListItem(
                title = "General settings",
                icon = {
                    Icon(
                        EvaIcons.Outline.Settings,
                        contentDescription = "General settings",
                    )
                },
                onClick = onNavigateToGeneralSettings,
            )
            IconListItem(
                title = "Timer style",
                icon = {
                    Icon(
                        EvaIcons.Outline.ColorPalette,
                        contentDescription = "Timer style",
                    )
                },
                onClick = onNavigateToTimerStyle,
            )
            IconListItem(
                title = "Notifications",
                icon = {
                    Icon(
                        EvaIcons.Outline.Bell,
                        contentDescription = "Notifications",
                    )
                },
                onClick = onNavigateToNotifications,
            )
        }
    }
}
