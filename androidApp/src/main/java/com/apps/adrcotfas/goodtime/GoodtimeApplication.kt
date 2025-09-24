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
import androidx.work.Configuration
import com.apps.adrcotfas.goodtime.backup.AutoBackupManager
import com.apps.adrcotfas.goodtime.backup.AutoBackupWorker
import com.apps.adrcotfas.goodtime.billing.BillingAbstract
import com.apps.adrcotfas.goodtime.bl.ALARM_MANAGER_HANDLER
import com.apps.adrcotfas.goodtime.bl.AlarmManagerHandler
import com.apps.adrcotfas.goodtime.bl.DND_MODE_MANAGER
import com.apps.adrcotfas.goodtime.bl.DndModeManager
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.SESSION_RESET_HANDLER
import com.apps.adrcotfas.goodtime.bl.SOUND_AND_VIBRATION_PLAYER
import com.apps.adrcotfas.goodtime.bl.SessionResetHandler
import com.apps.adrcotfas.goodtime.bl.TIMER_SERVICE_HANDLER
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerServiceStarter
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.SoundVibrationAndTorchPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.TorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.VibrationPlayer
import com.apps.adrcotfas.goodtime.data.backup.ActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.data.backup.AndroidBackupPrompter
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.di.DB_PATH_KEY
import com.apps.adrcotfas.goodtime.di.IO_SCOPE
import com.apps.adrcotfas.goodtime.di.WORKER_SCOPE
import com.apps.adrcotfas.goodtime.di.coreModule
import com.apps.adrcotfas.goodtime.di.coroutineScopeModule
import com.apps.adrcotfas.goodtime.di.getWith
import com.apps.adrcotfas.goodtime.di.localDataModule
import com.apps.adrcotfas.goodtime.di.mainModule
import com.apps.adrcotfas.goodtime.di.platformModule
import com.apps.adrcotfas.goodtime.di.timerManagerModule
import com.apps.adrcotfas.goodtime.di.viewModelModule
import com.apps.adrcotfas.goodtime.settings.notifications.SoundsViewModel
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderHelper
import com.apps.adrcotfas.goodtime.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.acra.ACRA
import org.acra.config.mailSender
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.koin.android.ext.android.get
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class GoodtimeApplication :
    Application(),
    KoinComponent,
    Configuration.Provider {
    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        if (ACRA.isACRASenderServiceProcess()) return

        startKoin {
            modules(
                module {
                    single<Context> { this@GoodtimeApplication }
                    single<ActivityResultLauncherManager> {
                        ActivityResultLauncherManager(
                            get(),
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }

                    single<BackupPrompter> {
                        AndroidBackupPrompter(get())
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
                            playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
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
                    single<EventListener>(named(EventListener.SESSION_RESET_HANDLER)) {
                        SessionResetHandler(get(), getWith("SessionResetHandler"))
                    }

                    single<EventListener>(named(EventListener.DND_MODE_MANAGER)) {
                        DndModeManager(
                            notificationManager = get<NotificationArchManager>(),
                            settingsRepository = get<SettingsRepository>(),
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }
                    single<EventListener>(named(EventListener.GLYPH_CONTROLLER)) {
                        com.apps.adrcotfas.goodtime.bl.NothingGlyphController(
                            context = get(),
                            settingsRepository = get(),
                            localDataRepository = get(),
                            timeProvider = get(),
                            logger = getWith("NothingGlyphController"),
                        )
                    }
                    single(createdAtStart = true) {
                        AutoBackupManager(
                            context = get(),
                            settingsRepository = get<SettingsRepository>(),
                            logger = getWith("AutoBackupManager"),
                        )
                    }
                    worker {
                        AutoBackupWorker(
                            get(),
                            get(),
                            get(),
                            getWith("AutoBackupWorker"),
                            get<String>(named(DB_PATH_KEY)),
                            get(),
                        )
                    }
                },
                flavorModule,
                coroutineScopeModule,
                platformModule,
                coreModule,
                localDataModule,
                timerManagerModule,
                viewModelModule,
                mainModule,
            )
            workManagerFactory()
        }

        val reminderHelper = get<ReminderHelper>()
        applicationScope.launch {
            reminderHelper.init()
        }
        val billing = get<BillingAbstract>()
        billing.init()
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)

        initAcra {
            alsoReportToAndroidFramework = true
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON

            notification {
                // required
                title = getString(R.string.main_crash_notification_title)
                // required
                text = getString(R.string.main_crash_notification_desc)
                // required
                channelName = getString(R.string.main_crash_channel_name)
                resSendButtonIcon = null
                resDiscardButtonIcon = null
            }
            mailSender {
                mailTo = getString(R.string.contact_address)
                subject = getString(R.string.crash_report_title)
                reportFileName = "crash.txt"
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            if (BuildConfig.DEBUG) {
                Configuration
                    .Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()
            } else {
                Configuration
                    .Builder()
                    .setMinimumLoggingLevel(android.util.Log.ERROR)
                    .build()
            }
}
