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
package com.apps.adrcotfas.goodtime.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.common.getVersionName
import com.apps.adrcotfas.goodtime.ui.common.BadgedBoxWithCount
import com.apps.adrcotfas.goodtime.ui.common.IconTextButton
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Info
import compose.icons.evaicons.outline.PieChart
import compose.icons.evaicons.outline.Settings
import compose.icons.evaicons.outline.Sync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationSheet(
    navController: NavController,
    onHideSheet: () -> Unit,
    settingsBadgeItemCount: Int,
    showPro: Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onHideSheet,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        MainNavigationSheetContent(
            settingsBadgeItemCount = settingsBadgeItemCount,
            showPro = showPro,
            navigateToLabels = {
                navController.navigate(LabelsDest)
                onHideSheet()
            },
            navigateToStats = {
                navController.navigate(StatsDest)
                onHideSheet()
            },
            navigateToSettings = {
                navController.navigate(SettingsDest)
                onHideSheet()
            },
            navigateToBackup = {
                navController.navigate(BackupDest)
                onHideSheet()
            },
            navigateToAbout = {
                navController.navigate(AboutDest)
                onHideSheet()
            },
            navigateToPro = {
                navController.navigate(ProDest)
                onHideSheet()
            },
        )
    }
}

@Composable
fun MainNavigationSheetContent(
    settingsBadgeItemCount: Int,
    showPro: Boolean,
    navigateToLabels: () -> Unit,
    navigateToStats: () -> Unit,
    navigateToSettings: () -> Unit,
    navigateToBackup: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToPro: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .animateContentSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Goodtime Productivity",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        if (showPro) {
            ProListItem { navigateToPro() }
        }
        IconTextButton(
            title = "Labels",
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = "Labels",
                )
            },
            onClick = navigateToLabels,
        )

        IconTextButton(
            title = "Statistics",
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.PieChart,
                    contentDescription = "Statistics",
                )
            },
            onClick = navigateToStats,
        )
        IconTextButton(
            title = "Backup and restore",
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Sync,
                    contentDescription = "Backup and restore",
                )
            },
            onClick = {
                navigateToBackup()
            },
        )
        SubtleHorizontalDivider()
        IconTextButton(
            title = "Settings",
            icon = {
                BadgedBoxWithCount(count = settingsBadgeItemCount) {
                    Icon(
                        imageVector = EvaIcons.Outline.Settings,
                        contentDescription = "Settings",
                    )
                }
            },
            onClick = navigateToSettings,
        )
        IconTextButton(
            title = "About and feedback",
            subtitle = "v${context.getVersionName()}",
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Info,
                    contentDescription = "About and feedback",
                )
            },
            onClick = {
                navigateToAbout()
            },
        )
    }
}
