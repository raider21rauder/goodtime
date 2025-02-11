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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults.colors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.common.BadgedBoxWithCount
import com.apps.adrcotfas.goodtime.common.getVersionName
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Info
import compose.icons.evaicons.outline.PieChart
import compose.icons.evaicons.outline.Settings
import compose.icons.evaicons.outline.Sync

@Composable
private fun IconListItem(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick),
        colors = colors(containerColor = Color.Transparent),
        headlineContent = { Text(text = title) },
        supportingContent = {
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        leadingContent = icon,
    )
}

@Composable
fun MainNavigationSheet(
    settingsBadgeItemCount: Int,
    navigateToLabels: () -> Unit,
    navigateToStats: () -> Unit,
    navigateToSettings: () -> Unit,
    navigateToBackup: () -> Unit,
    navigateToAbout: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(vertical = 16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        val modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        IconListItem(
            title = "Labels",
            icon = {
                Icon(
                    modifier = modifier,
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = "Labels",
                )
            },
            onClick = navigateToLabels,
        )
        IconListItem(
            title = "Statistics",
            icon = {
                Icon(
                    modifier = modifier,
                    imageVector = EvaIcons.Outline.PieChart,
                    contentDescription = "Statistics",
                )
            },
            onClick = navigateToStats,
        )
        IconListItem(
            title = "Backup and restore",
            icon = {
                Icon(
                    modifier = modifier,
                    imageVector = EvaIcons.Outline.Sync,
                    contentDescription = "Backup and restore",
                )
            },
            onClick = {
                navigateToBackup()
            },
        )
        SubtleHorizontalDivider()
        IconListItem(
            title = "Settings",
            icon = {
                BadgedBoxWithCount(modifier = modifier, count = settingsBadgeItemCount) {
                    Icon(
                        imageVector = EvaIcons.Outline.Settings,
                        contentDescription = "Settings",
                    )
                }
            },
            onClick = navigateToSettings,
        )
        IconListItem(
            title = "About and feedback",
            subtitle = "Goodtime Productivity ${context.getVersionName()}",
            icon = {
                Icon(
                    modifier = modifier,
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
