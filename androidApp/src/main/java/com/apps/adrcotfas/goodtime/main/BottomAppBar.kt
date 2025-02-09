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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.common.BadgedBoxWithCount
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Menu2
import kotlinx.coroutines.launch

@Composable
fun BottomAppBar(
    modifier: Modifier,
    badgeItemCount: Int,
    hide: Boolean,
    labelColor: Color,
    onShowSheet: () -> Unit,
    navController: NavController,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = !hide,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // TODO: consider camera cutouts when in landscape
        // TODO: add badge for actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onShowSheet,
            ) {
                BadgedBoxWithCount(count = badgeItemCount) {
                    Icon(
                        imageVector = EvaIcons.Outline.Menu2,
                        contentDescription = "Open app menu",
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                // TODO: open a select dialog instead but have a button to go to Labels
                navController.navigate(LabelsDest)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = "Labels",
                    tint = labelColor,
                )
            }
            IconButton(onClick = {
                navController.navigate(StatsDest)
            }) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
                        ),
                ) {
                    // TODO: current stats of the day
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationSheet(
    navController: NavController,
    onHideSheet: () -> Unit,
    settingsBadgeItemCount: Int,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideNavigationSheet = {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onHideSheet()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onHideSheet,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        MainNavigationSheet(
            settingsBadgeItemCount = settingsBadgeItemCount,
            navigateToLabels = {
                navController.navigate(LabelsDest)
                hideNavigationSheet()
            },
            navigateToStats = {
                navController.navigate(StatsDest)
                hideNavigationSheet()
            },
            navigateToSettings = {
                navController.navigate(SettingsDest)
                hideNavigationSheet()
            },
            navigateToBackup = {
                navController.navigate(BackupDest)
                hideNavigationSheet()
            },
            navigateToAbout = {
                navController.navigate(AboutDest)
                hideNavigationSheet()
            },
        )
    }
}
