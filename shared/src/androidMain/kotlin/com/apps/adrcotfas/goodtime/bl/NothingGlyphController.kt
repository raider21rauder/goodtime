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
package com.apps.adrcotfas.goodtime.bl

import android.content.ComponentName
import android.content.Context
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.streakInUse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Controls the Nothing Glyph Interface on Phone (3a) via reflection, so the app builds on non-Nothing devices.
 *
 * Phone (3a) mapping per SDK docs:
 * C1 - C20 => indices 0..19 (progress arc)
 * A1 - A11 => indices 20..30 (pomodori progress to next long break)
 * B1 - B5  => indices 31..35 (break/pause indicators)
 */
class NothingGlyphController(
    private val context: Context,
    settingsRepository: SettingsRepository,
    localDataRepository: LocalDataRepository,
    private val timeProvider: TimeProvider,
    private val logger: Logger,
) : EventListener {
    private val scope: CoroutineScope = MainScope()

    // Latest label/break settings (sessions before long break, long-break enabled)
    private data class BreakConfig(
        val isCountdown: Boolean = true,
        val workDurationMin: Int = 25,
        val breakDurationMin: Int = 5,
        val longBreakDurationMin: Int = 15,
        val sessionsBeforeLongBreak: Int = 4,
        val isLongBreakEnabled: Boolean = true,
        val longBreakData: LongBreakData = LongBreakData(),
    )

    private val breakConfigState =
        combine(
            settingsRepository.settings,
            settingsRepository.settings.map { it.labelName }.distinctUntilChanged()
                .flatMapLatest { labelName ->
                    combine(
                        localDataRepository.selectLabelByName(labelName),
                        localDataRepository.selectDefaultLabel().filterNotNull(),
                    ) { label, defaultLabel ->
                        val profile = (label ?: defaultLabel).timerProfile
                        profile
                    }
                },
        ) { settings, profile ->
            BreakConfig(
                isCountdown = profile.isCountdown,
                workDurationMin = profile.workDuration,
                breakDurationMin = profile.breakDuration,
                longBreakDurationMin = profile.longBreakDuration,
                sessionsBeforeLongBreak = profile.sessionsBeforeLongBreak,
                isLongBreakEnabled = profile.isLongBreakEnabled,
                longBreakData = settings.longBreakData,
            )
        }.stateIn(scope, SharingStarted.Eagerly, BreakConfig())

    // Reflection handles
    private var gm: Any? = null
    private var glyphClass: Class<*>? = null
    private var commonClass: Class<*>? = null
    private var frameClass: Class<*>? = null
    private var builderClass: Class<*>? = null
    private var callbackProxy: Any? = null

    private var progressJob: Job? = null
    private var pulseJob: Job? = null
    private var currentEndTime: Long = 0L
    private var currentTotalMs: Long = 0L
    private var isRunning: Boolean = false

    init {
        initGlyphIfAvailable()
    }

    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start -> onStart(event)
            Event.Pause -> onPause()
            is Event.AddOneMinute -> onAddOneMinute(event)
            is Event.Finished -> onFinished()
            Event.Reset -> onReset()
            is Event.SendToBackground -> {}
            Event.BringToForeground -> {}
            Event.UpdateActiveLabel -> updateAProgress()
        }
    }

    private fun onStart(event: Event.Start) {
        if (gm == null) return
        isRunning = true
        currentEndTime = event.endTime
        stopPulse()

        val cfg = breakConfigState.value
        if (event.isFocus) {
            // Focus session: show A progress towards long break, ensure B off
            toggleChannels(emptyList())
            updateAProgress()
        } else {
            // Break session
            val isLongBreak = cfg.isLongBreakEnabled && cfg.sessionsBeforeLongBreak > 0 && cfg.longBreakData.streakInUse(cfg.sessionsBeforeLongBreak) == 0
            if (isLongBreak) {
                // Long break: B1 solid, A pulsates back-and-forth
                toggleChannels(listOf(bIndex(1)))
                startAPulse()
            } else {
                // Short break: B1..B5 solid
                toggleChannels((1..5).map { bIndex(it) })
            }
        }

        // Compute total duration for accurate C progress, only meaningful on countdown profiles
        currentTotalMs = when {
            !cfg.isCountdown -> 0L
            event.isFocus -> cfg.workDurationMin * 60_000L
            else -> {
                val isLongBreak = cfg.isLongBreakEnabled && cfg.sessionsBeforeLongBreak > 0 && cfg.longBreakData.streakInUse(cfg.sessionsBeforeLongBreak) == 0
                (if (isLongBreak) cfg.longBreakDurationMin else cfg.breakDurationMin) * 60_000L
            }
        }

        startProgressLoop()
    }

    private fun onPause() { /* No-op: B lights are for break sessions, not paused state */ }

    private fun onAddOneMinute(event: Event.AddOneMinute) {
        currentEndTime = event.endTime
        // ensure loop continues (it might be running already)
        if (isRunning && progressJob == null) startProgressLoop()
    }

    private fun onFinished() {
        isRunning = false
        stopProgress()
        stopPulse()
        turnOff()
    }

    private fun onReset() {
        isRunning = false
        stopProgress()
        stopPulse()
        turnOff()
    }

    private fun startProgressLoop() {
        stopProgress()
        progressJob = scope.launch {
            while (isActive) {
                displayProgress()
                delay(500L)
            }
        }
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun startAPulse() {
        stopPulse()
        pulseJob = scope.launch {
            var forward = true
            var pos = 1
            while (isActive) {
                val channels = listOf(aIndex(pos))
                animateChannels(channels, period = 500, cycles = 1, interval = 0)
                delay(150L)
                pos = if (forward) pos + 1 else pos - 1
                if (pos >= 11) { pos = 11; forward = false }
                if (pos <= 1) { pos = 1; forward = true }
            }
        }
    }

    private fun stopPulse() {
        pulseJob?.cancel()
        pulseJob = null
    }

    private fun displayProgress() {
        try {
            val manager = gm ?: return
            val builder = getBuilder() ?: return
            val frame = builderBuild(builder)

            val now = timeProvider.elapsedRealtime()
            val remaining = (currentEndTime - now).coerceAtLeast(0L)
            val progress = if (currentTotalMs > 0L) {
                val done = (currentTotalMs - remaining).coerceIn(0L, currentTotalMs)
                ((done * 100L) / currentTotalMs).toInt().coerceIn(0, 100)
            } else {
                // Fallback when profile is count-up: animate breathing on C instead of progress bar
                animateChannels((1..20).map { it - 1 }, period = 800, cycles = 1, interval = 0)
                return
            }
            gmCall("displayProgress", arrayOf(frameClass, Int::class.javaPrimitiveType), arrayOf(frame, progress))
        } catch (t: Throwable) {
            logger.w(t) { "displayProgress failed" }
        }
    }

    private fun updateAProgress() {
        try {
            val cfg = breakConfigState.value
            if (cfg.sessionsBeforeLongBreak <= 0) return
            val done = cfg.longBreakData.streakInUse(cfg.sessionsBeforeLongBreak)
            val lit = ((done.toFloat() / cfg.sessionsBeforeLongBreak.toFloat()) * 11f).roundToInt().coerceIn(0, 11)
            val channels = (1..lit).map { aIndex(it) }
            toggleChannels(channels)
        } catch (t: Throwable) {
            logger.w(t) { "updateAProgress failed" }
        }
    }

    private fun toggleChannels(indices: List<Int>) {
        try {
            val builder = getBuilder() ?: return
            // If no channels, turn all off
            if (indices.isEmpty()) {
                turnOff(); return
            }
            indices.forEach { idx -> builderBuildChannel(builder, idx) }
            val frame = builderBuild(builder)
            gmCall("toggle", arrayOf(frameClass), arrayOf(frame))
        } catch (t: Throwable) {
            logger.w(t) { "toggleChannels failed" }
        }
    }

    private fun animateChannels(indices: List<Int>, period: Int, cycles: Int, interval: Int) {
        try {
            val builder = getBuilder() ?: return
            indices.forEach { idx -> builderBuildChannel(builder, idx) }
            builderCall(builder, "buildPeriod", arrayOf(Int::class.javaPrimitiveType), arrayOf(period))
            builderCall(builder, "buildCycles", arrayOf(Int::class.javaPrimitiveType), arrayOf(cycles))
            builderCall(builder, "buildInterval", arrayOf(Int::class.javaPrimitiveType), arrayOf(interval))
            val frame = builderBuild(builder)
            gmCall("animate", arrayOf(frameClass), arrayOf(frame))
        } catch (t: Throwable) {
            logger.w(t) { "animateChannels failed" }
        }
    }

    private fun turnOff() {
        try {
            gmCall("turnOff", emptyArray(), emptyArray())
        } catch (t: Throwable) {
            logger.w(t) { "turnOff failed" }
        }
    }

    private fun initGlyphIfAvailable() {
        try {
            glyphClass = Class.forName("com.nothing.ketchum.Glyph")
            commonClass = Class.forName("com.nothing.ketchum.Common")
            frameClass = Class.forName("com.nothing.ketchum.GlyphFrame")
            builderClass = Class.forName("com.nothing.ketchum.GlyphFrame$Builder")
            val managerClass = Class.forName("com.nothing.ketchum.GlyphManager")

            gm = managerClass.getMethod("getInstance", Context::class.java).invoke(null, context.applicationContext)

            // Setup callback proxy
            val callbackClass = Class.forName("com.nothing.ketchum.GlyphManager$Callback")
            callbackProxy = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                when (method.name) {
                    "onServiceConnected" -> {
                        onServiceConnected(args?.get(0) as? ComponentName)
                        null
                    }
                    "onServiceDisconnected" -> {
                        onServiceDisconnected(args?.get(0) as? ComponentName)
                        null
                    }
                    else -> null
                }
            }

            // init service
            gmCall("init", arrayOf(callbackClass), arrayOf(callbackProxy!!))
        } catch (t: Throwable) {
            // No-op on non-Nothing devices
            gm = null
            logger.i { "Glyph SDK not available: ${t.javaClass.simpleName}" }
        }
    }

    private fun onServiceConnected(@Suppress("UNUSED_PARAMETER") componentName: ComponentName?) {
        try {
            val device24111 = glyphClass!!.getField("DEVICE_24111").get(null)
            val registered = gmCall("register", arrayOf(String::class.java), arrayOf(device24111)) as? Boolean
            if (registered == true) {
                gmCall("openSession", emptyArray(), emptyArray())
                logger.i { "Glyph session opened" }
            }
        } catch (t: Throwable) {
            logger.w(t) { "Glyph register/open failed" }
        }
    }

    private fun onServiceDisconnected(@Suppress("UNUSED_PARAMETER") componentName: ComponentName?) {
        try {
            gmCall("closeSession", emptyArray(), emptyArray())
        } catch (_: Throwable) {}
    }

    private fun getBuilder(): Any? {
        return try {
            gmCall("getGlyphFrameBuilder", emptyArray(), emptyArray())
        } catch (t: Throwable) {
            logger.w(t) { "getGlyphFrameBuilder failed" }
            null
        }
    }

    private fun builderBuildChannel(builder: Any, channelIndex: Int) {
        builderCall(builder, "buildChannel", arrayOf(Int::class.javaPrimitiveType), arrayOf(channelIndex))
    }

    private fun builderBuild(builder: Any): Any {
        return builderCall(builder, "build", emptyArray(), emptyArray())
    }

    private fun builderCall(builder: Any, name: String, paramTypes: Array<Class<*>?>, args: Array<Any?>): Any {
        val method = builderClass!!.getMethod(name, *paramTypes)
        return method.invoke(builder, *args)
    }

    private fun gmCall(name: String, paramTypes: Array<Class<*>?>, args: Array<Any?>): Any? {
        val method = gm!!.javaClass.getMethod(name, *paramTypes)
        return method.invoke(gm, *args)
    }

    private fun aIndex(pos: Int): Int = 19 + pos // 20..30
    private fun bIndex(pos: Int): Int = 30 + pos // 31..35

    fun destroy() {
        scope.cancel()
        try { gmCall("closeSession", emptyArray(), emptyArray()) } catch (_: Throwable) {}
        try { gmCall("unInit", emptyArray(), emptyArray()) } catch (_: Throwable) {}
    }
}

