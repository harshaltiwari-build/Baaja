/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.metrolist.music.playback.alarm.MusicAlarmScheduler
import com.metrolist.music.playback.alarm.MusicAlarmStore
import timber.log.Timber
import kotlin.concurrent.thread

/** Base for the Baaja widgets: each one redraws itself from [BaajaWidgetState] whenever Android asks. */
abstract class BaajaWidgetReceiver : AppWidgetProvider() {
    abstract fun render(context: Context)

    // A problem while drawing one widget must never take the whole app down.
    private fun safeRender(context: Context) {
        runCatching { render(context) }.onFailure { Timber.tag("BaajaWidget").w(it, "Widget render failed") }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = safeRender(context)

    override fun onEnabled(context: Context) = safeRender(context)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = safeRender(context)
}

class SleepWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderSleep(context)
}

class ResumeWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderResume(context)
}

class UpNextWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderUpNext(context)
}

class SearchWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderSearch(context)
}

class QuickPlayWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderQuick(context)
}

class TogetherWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderTogether(context)
}

/** The alarm widget switches the next alarm on or off without opening the app. */
class AlarmWidgetReceiver : BaajaWidgetReceiver() {
    override fun render(context: Context) = BaajaWidgetRenderer.renderAlarm(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != BaajaWidgetRenderer.ACTION_ALARM_TOGGLE) return
        val pending: BroadcastReceiver.PendingResult = goAsync()
        thread {
            try {
                val alarms = MusicAlarmStore.loadBlocking(context)
                val target = alarms.filter { it.enabled && it.nextTriggerAt > 0 }.minByOrNull { it.nextTriggerAt } ?: alarms.firstOrNull()
                if (target != null) {
                    val updated = alarms.map { if (it.id == target.id) it.copy(enabled = !it.enabled) else it }
                    MusicAlarmScheduler.scheduleAll(context, updated)
                }
                BaajaWidgetRenderer.renderAlarm(context)
            } catch (e: Exception) {
                Timber.tag("BaajaWidget").w(e, "Alarm toggle failed")
            } finally {
                pending.finish()
            }
        }
    }
}
