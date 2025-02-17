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
package com.apps.adrcotfas.goodtime

import android.app.Application
import android.content.Context
import com.apps.adrcotfas.goodtime.billing.BillingAbstract
import com.apps.adrcotfas.goodtime.bl.ALARM_MANAGER_HANDLER
import com.apps.adrcotfas.goodtime.bl.AlarmManagerHandler
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.SOUND_AND_VIBRATION_PLAYER
import com.apps.adrcotfas.goodtime.bl.TIMER_SERVICE_HANDLER
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerServiceStarter
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.SoundVibrationAndTorchPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.TorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.VibrationPlayer
import com.apps.adrcotfas.goodtime.data.backup.AndroidBackupPrompter
import com.apps.adrcotfas.goodtime.data.backup.RestoreActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.di.IO_SCOPE
import com.apps.adrcotfas.goodtime.di.WORKER_SCOPE
import com.apps.adrcotfas.goodtime.di.getWith
import com.apps.adrcotfas.goodtime.di.insertKoin
import com.apps.adrcotfas.goodtime.settings.notifications.SoundsViewModel
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class GoodtimeApplication : Application() {
    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        insertKoin(
            module {
                single<Context> { this@GoodtimeApplication }
                single(createdAtStart = true) { ActivityProvider(this@GoodtimeApplication) }
                single<RestoreActivityResultLauncherManager> {
                    RestoreActivityResultLauncherManager(
                        get(),
                        coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                    )
                }

                single<BackupPrompter> {
                    AndroidBackupPrompter(get(), get(), get())
                }
                single<NotificationArchManager> {
                    NotificationArchManager(
                        get<Context>(),
                        MainActivity::class.java,
                    )
                }
                single<EventListener>(named(EventListener.TIMER_SERVICE_HANDLER)) {
                    TimerServiceStarter(get())
                }
                single<EventListener>(named(EventListener.ALARM_MANAGER_HANDLER)) {
                    AlarmManagerHandler(
                        get<Context>(),
                        get<TimeProvider>(),
                        getWith("AlarmManagerHandler"),
                    )
                }
                single<ReminderHelper> {
                    ReminderHelper(
                        get(),
                        get(),
                        getWith("ReminderHelper"),
                    )
                }
                viewModel<SoundsViewModel> {
                    SoundsViewModel(
                        settingsRepository = get(),
                    )
                }
                single {
                    SoundPlayer(
                        context = get(),
                        ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                        playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                        settingsRepo = get(),
                        logger = getWith("SoundPlayer"),
                    )
                }
                single {
                    VibrationPlayer(
                        context = get(),
                        ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                        settingsRepo = get(),
                    )
                }
                single {
                    TorchManager(
                        context = get(),
                        ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                        playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                        settingsRepo = get(),
                        logger = getWith("TorchManager"),
                    )
                }
                single<EventListener>(named(EventListener.SOUND_AND_VIBRATION_PLAYER)) {
                    SoundVibrationAndTorchPlayer(
                        soundPlayer = get(),
                        vibrationPlayer = get(),
                        torchManager = get(),
                    )
                }
            },
            flavorModule,
        )
        val reminderHelper = get<ReminderHelper>()
        applicationScope.launch {
            reminderHelper.init()
        }
        val billing = get<BillingAbstract>()
        billing.init()
    }
}
