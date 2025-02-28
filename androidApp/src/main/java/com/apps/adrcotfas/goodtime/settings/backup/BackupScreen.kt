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
package com.apps.adrcotfas.goodtime.settings.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.backup.RestoreActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.data.local.backup.BackupViewModel
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.ActionCard
import com.apps.adrcotfas.goodtime.ui.common.CircularProgressListItem
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Unlock
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = koinInject(),
    activityResultLauncherManager: RestoreActivityResultLauncherManager = koinInject(),
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val enabled = uiState.isPro

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                activityResultLauncherManager.importCallback(it)
            }
        },
    )

    LaunchedEffect(Unit) {
        activityResultLauncherManager.setImportActivityResultLauncher(restoreBackupLauncher)
    }

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED, Lifecycle.State.CREATED -> {
                viewModel.clearProgress()
            }

            else -> {
                // do nothing
            }
        }
    }

    LaunchedEffect(uiState.backupResult) {
        uiState.backupResult?.let {
            Toast.makeText(
                context,
                if (it) {
                    context.getString(R.string.backup_completed_successfully)
                } else {
                    context.getString(
                        R.string.backup_failed_please_try_again,
                    )
                },
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.clearBackupError()
        }
    }

    LaunchedEffect(uiState.restoreResult) {
        uiState.restoreResult?.let {
            Toast.makeText(
                context,
                if (it) {
                    context.getString(R.string.backup_restore_completed_successfully)
                } else {
                    context.getString(
                        R.string.backup_restore_failed_please_try_again,
                    )
                },
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.clearRestoreError()
        }
    }
    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.backup_and_restore_title),
                onNavigateBack = { onNavigateBack() },
                showSeparator = listState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(listState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (!enabled) {
                ActionCard(icon = {
                    Icon(
                        imageVector = EvaIcons.Outline.Unlock,
                        contentDescription = context.getString(R.string.unlock_premium),
                    )
                }, description = stringResource(R.string.unlock_premium_to_access_features)) {
                    onNavigateToPro()
                }
            }

            CircularProgressListItem(
                title = stringResource(R.string.backup_export_backup),
                subtitle = stringResource(R.string.backup_the_file_can_be_imported_back),
                enabled = enabled,
                showProgress = uiState.isBackupInProgress,
            ) {
                viewModel.backup()
            }
            CircularProgressListItem(
                title = stringResource(R.string.backup_restore_backup),
                enabled = enabled,
                showProgress = uiState.isRestoreInProgress,
            ) {
                viewModel.restore()
            }
            SubtleHorizontalDivider()
            CircularProgressListItem(
                title = stringResource(R.string.backup_export_csv),
                enabled = enabled,
                showProgress = uiState.isCsvBackupInProgress,
            ) {
                viewModel.backupToCsv()
            }
            CircularProgressListItem(
                title = stringResource(R.string.backup_export_json),
                enabled = enabled,
                showProgress = uiState.isJsonBackupInProgress,
            ) {
                viewModel.backupToJson()
            }
        }
    }
}
