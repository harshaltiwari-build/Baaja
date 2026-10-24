/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.alarm.MusicAlarmStore
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** What the widgets last learned about playback, kept on disk so they can draw without the service. */
object BaajaWidgetState {
    private const val PREFS = "baaja_widget_state"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveLast(context: Context, title: String, artist: String, position: Long, duration: Long, playing: Boolean) {
        prefs(context).edit()
            .putString("last_title", title)
            .putString("last_artist", artist)
            .putLong("last_pos", position)
            .putLong("last_dur", duration)
            .putBoolean("last_playing", playing)
            .apply()
    }

    fun saveSleep(context: Context, endAt: Long, untilSongEnd: Boolean) {
        prefs(context).edit().putLong("sleep_end", endAt).putBoolean("sleep_song", untilSongEnd).apply()
    }

    fun saveTogether(context: Context, code: String, count: Int) {
        prefs(context).edit().putString("tg_code", code).putInt("tg_count", count).apply()
    }

    fun saveLiked(context: Context, count: Int) {
        prefs(context).edit().putInt("liked_count", count).apply()
    }

    fun saveQueue(context: Context, titles: List<String>, artists: List<String>, total: Int) {
        val e = prefs(context).edit()
        e.putInt("q_n", titles.size).putInt("q_total", total)
        titles.forEachIndexed { i, t -> e.putString("q_t$i", t).putString("q_a$i", artists.getOrNull(i).orEmpty()) }
        e.apply()
    }

    fun lastTitle(c: Context): String? = prefs(c).getString("last_title", null)
    fun lastArtist(c: Context): String = prefs(c).getString("last_artist", "").orEmpty()
    fun lastPos(c: Context): Long = prefs(c).getLong("last_pos", 0L)
    fun lastDur(c: Context): Long = prefs(c).getLong("last_dur", 0L)
    fun lastPlaying(c: Context): Boolean = prefs(c).getBoolean("last_playing", false)
    fun sleepEnd(c: Context): Long = prefs(c).getLong("sleep_end", -1L)
    fun sleepSong(c: Context): Boolean = prefs(c).getBoolean("sleep_song", false)
    fun togetherCode(c: Context): String = prefs(c).getString("tg_code", "").orEmpty()
    fun togetherCount(c: Context): Int = prefs(c).getInt("tg_count", 0)
    fun likedCount(c: Context): Int = prefs(c).getInt("liked_count", -1)
    fun queueSize(c: Context): Int = prefs(c).getInt("q_n", 0)
    fun queueTotal(c: Context): Int = prefs(c).getInt("q_total", 0)
    fun queueTitle(c: Context, i: Int): String = prefs(c).getString("q_t$i", "").orEmpty()
    fun queueArtist(c: Context, i: Int): String = prefs(c).getString("q_a$i", "").orEmpty()

    fun coverFile(context: Context, name: String) = File(context.cacheDir, "widget_cover_$name.png")
}

/** Draws the Baaja widgets that are not the main player. Safe to call from any receiver or the service. */
object BaajaWidgetRenderer {
    const val ACTION_SLEEP_SET = "com.baaja.music.widget.SLEEP_SET"
    const val ACTION_SLEEP_ADD = "com.baaja.music.widget.SLEEP_ADD"
    const val ACTION_SLEEP_CANCEL = "com.baaja.music.widget.SLEEP_CANCEL"
    const val ACTION_SLEEP_TOGGLE = "com.baaja.music.widget.SLEEP_TOGGLE"
    const val ACTION_JUMP = "com.baaja.music.widget.JUMP"
    const val ACTION_ALARM_TOGGLE = "com.baaja.music.widget.ALARM_TOGGLE"
    const val EXTRA_MINUTES = "bw_minutes"
    const val EXTRA_OFFSET = "bw_offset"

    private const val ACTION_SEARCH = "com.baaja.music.action.SEARCH"
    private const val ACTION_RECOGNITION = "com.baaja.music.action.RECOGNITION"

    fun renderAll(context: Context) {
        renderSleep(context)
        renderAlarm(context)
        renderUpNext(context)
        renderSearch(context)
        renderQuick(context)
        renderTogether(context)
        renderResume(context)
    }

    private fun ids(context: Context, cls: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))

    private fun push(context: Context, cls: Class<*>, views: RemoteViews) {
        val ids = ids(context, cls)
        if (ids.isNotEmpty()) AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
    }

    private fun serviceIntent(context: Context, code: Int, action: String, extras: Intent.() -> Unit = {}): PendingIntent {
        val intent = Intent(context, MusicService::class.java).setAction(action).apply(extras)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, code, intent, flags)
        } else {
            PendingIntent.getService(context, code, intent, flags)
        }
    }

    private fun activityIntent(context: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun openApp(context: Context): Intent =
        Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun formatClock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    // ---- Sleep timer ----
    fun renderSleep(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_sleep)
        val end = BaajaWidgetState.sleepEnd(context)
        val remaining = end - System.currentTimeMillis()
        val songMode = BaajaWidgetState.sleepSong(context)
        val timed = end > 0 && remaining > 0
        if (timed || songMode) {
            views.setViewVisibility(R.id.bw_sleep_idle, View.GONE)
            views.setViewVisibility(R.id.bw_sleep_active, View.VISIBLE)
            views.setTextViewText(R.id.bw_sleep_left, if (timed) formatClock(remaining) else context.getString(R.string.bw_song_end))
            views.setTextViewText(R.id.bw_sleep_left_label, context.getString(if (timed) R.string.bw_left else R.string.bw_sleep_timer))
            views.setOnClickPendingIntent(R.id.bw_sleep_add, serviceIntent(context, 20, ACTION_SLEEP_ADD))
            views.setOnClickPendingIntent(R.id.bw_sleep_cancel, serviceIntent(context, 21, ACTION_SLEEP_CANCEL))
        } else {
            views.setViewVisibility(R.id.bw_sleep_idle, View.VISIBLE)
            views.setViewVisibility(R.id.bw_sleep_active, View.GONE)
            listOf(R.id.bw_sleep_15 to 15, R.id.bw_sleep_30 to 30, R.id.bw_sleep_60 to 60, R.id.bw_sleep_song to -1).forEach { (id, minutes) ->
                views.setOnClickPendingIntent(id, serviceIntent(context, 30 + (minutes + 1), ACTION_SLEEP_SET) { putExtra(EXTRA_MINUTES, minutes) })
            }
        }
        push(context, SleepWidgetReceiver::class.java, views)
    }

    // ---- Music alarm ----
    fun renderAlarm(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_alarm)
        val alarms = MusicAlarmStore.loadBlocking(context)
        val next = alarms.filter { it.enabled && it.nextTriggerAt > 0 }.minByOrNull { it.nextTriggerAt }
        val shown = next ?: alarms.firstOrNull()
        if (shown == null) {
            views.setTextViewText(R.id.bw_alarm_time, "--:--")
            views.setTextViewText(R.id.bw_alarm_sub, context.getString(R.string.bw_no_alarm))
            views.setTextViewText(R.id.bw_alarm_day, "")
            views.setViewVisibility(R.id.bw_alarm_toggle, View.GONE)
            views.setOnClickPendingIntent(android.R.id.background, activityIntent(context, 50, openApp(context)))
        } else {
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, shown.hour); set(Calendar.MINUTE, shown.minute) }
            views.setTextViewText(R.id.bw_alarm_time, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(cal.timeInMillis)))
            views.setViewVisibility(R.id.bw_alarm_toggle, View.VISIBLE)
            views.setTextViewText(R.id.bw_alarm_toggle, context.getString(if (shown.enabled) R.string.bw_on else R.string.bw_off))
            views.setInt(R.id.bw_alarm_toggle, "setBackgroundResource", if (shown.enabled) R.drawable.bw_chip_on else R.drawable.bw_chip)
            views.setTextColor(R.id.bw_alarm_toggle, context.getColor(if (shown.enabled) R.color.bw_on_purple else R.color.bw_ink))
            views.setTextViewText(R.id.bw_alarm_sub, context.getString(R.string.bw_next_alarm).lowercase().replaceFirstChar { it.uppercase() })
            views.setTextViewText(R.id.bw_alarm_day, if (shown.enabled && shown.nextTriggerAt > 0) dayLabel(context, shown.nextTriggerAt) else "")
            val toggle =
                PendingIntent.getBroadcast(
                    context, 51,
                    Intent(context, AlarmWidgetReceiver::class.java).setAction(ACTION_ALARM_TOGGLE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.bw_alarm_toggle, toggle)
            views.setOnClickPendingIntent(android.R.id.background, activityIntent(context, 50, openApp(context)))
        }
        push(context, AlarmWidgetReceiver::class.java, views)
    }

    private fun dayLabel(context: Context, at: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = at }
        val sameDay = now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        return if (sameDay) "Today" else target.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()).orEmpty()
    }

    // ---- Up next ----
    fun renderUpNext(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_upnext)
        val n = BaajaWidgetState.queueSize(context).coerceAtMost(4)
        val rowIds = listOf(R.id.bw_un_row1, R.id.bw_un_row2, R.id.bw_un_row3, R.id.bw_un_row4)
        val artIds = listOf(R.id.bw_un_art1, R.id.bw_un_art2, R.id.bw_un_art3, R.id.bw_un_art4)
        val titleIds = listOf(R.id.bw_un_title1, R.id.bw_un_title2, R.id.bw_un_title3, R.id.bw_un_title4)
        val artistIds = listOf(R.id.bw_un_artist1, R.id.bw_un_artist2, R.id.bw_un_artist3, R.id.bw_un_artist4)
        views.setViewVisibility(R.id.bw_un_empty, if (n == 0) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.bw_un_meta, if (n == 0) "" else context.getString(R.string.bw_songs_count, BaajaWidgetState.queueTotal(context)))
        for (i in 0 until 4) {
            if (i >= n) {
                views.setViewVisibility(rowIds[i], View.GONE)
                continue
            }
            views.setViewVisibility(rowIds[i], View.VISIBLE)
            views.setTextViewText(titleIds[i], BaajaWidgetState.queueTitle(context, i))
            views.setTextViewText(artistIds[i], BaajaWidgetState.queueArtist(context, i))
            val cover = BaajaWidgetState.coverFile(context, "q$i")
            if (cover.exists()) {
                BitmapFactory.decodeFile(cover.path)?.let { views.setImageViewBitmap(artIds[i], it) }
            }
            views.setInt(rowIds[i], "setBackgroundResource", if (i == 0) R.drawable.bw_row_hi else 0)
            views.setOnClickPendingIntent(rowIds[i], serviceIntent(context, 60 + i, ACTION_JUMP) { putExtra(EXTRA_OFFSET, i) })
        }
        push(context, UpNextWidgetReceiver::class.java, views)
    }

    // ---- Search and identify ----
    fun renderSearch(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_search)
        views.setOnClickPendingIntent(R.id.bw_search_pill, activityIntent(context, 70, openApp(context).setAction(ACTION_SEARCH)))
        views.setOnClickPendingIntent(
            R.id.bw_search_mic,
            activityIntent(context, 71, openApp(context).setAction(ACTION_RECOGNITION).putExtra(MainActivity.EXTRA_AUTO_START_RECOGNITION, true)),
        )
        push(context, SearchWidgetReceiver::class.java, views)
    }

    // ---- Quick play ----
    fun renderQuick(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_quick)
        val liked = BaajaWidgetState.likedCount(context)
        if (liked >= 0) views.setTextViewText(R.id.bw_q_liked_sub, context.getString(R.string.bw_songs_count, liked))
        fun target(code: Int, type: String, id: String = "") =
            serviceIntent(context, code, PlaylistWidgetReceiver.ACTION_PLAY_TARGET) {
                putExtra(PlaylistWidgetReceiver.EXTRA_TARGET_TYPE, type)
                putExtra(PlaylistWidgetReceiver.EXTRA_TARGET_ID, id)
            }
        views.setOnClickPendingIntent(R.id.bw_q_liked, target(80, PlaylistWidgetReceiver.TARGET_TYPE_LIKED))
        views.setOnClickPendingIntent(R.id.bw_q_down, target(81, PlaylistWidgetReceiver.TARGET_TYPE_DOWNLOADED))
        views.setOnClickPendingIntent(R.id.bw_q_mix, target(82, PlaylistWidgetReceiver.TARGET_TYPE_TOP, "50"))
        push(context, QuickPlayWidgetReceiver::class.java, views)
    }

    // ---- Listen together ----
    fun renderTogether(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_together)
        val code = BaajaWidgetState.togetherCode(context)
        if (code.isNotBlank()) {
            views.setTextViewText(R.id.bw_tg_label, context.getString(R.string.bw_listening_together))
            views.setTextViewText(R.id.bw_tg_code, code)
            views.setTextViewText(R.id.bw_tg_sub, context.getString(R.string.bw_here, BaajaWidgetState.togetherCount(context)))
            views.setViewVisibility(R.id.bw_tg_share, View.VISIBLE)
            views.setTextViewText(R.id.bw_tg_main, context.getString(R.string.bw_open_room))
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, context.getString(R.string.bw_share_text, code))
            views.setOnClickPendingIntent(R.id.bw_tg_share, activityIntent(context, 90, Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
        } else {
            views.setTextViewText(R.id.bw_tg_label, context.getString(R.string.bw_listen_together))
            views.setTextViewText(R.id.bw_tg_code, context.getString(R.string.bw_same_song))
            views.setTextViewText(R.id.bw_tg_sub, context.getString(R.string.bw_start_hint))
            views.setViewVisibility(R.id.bw_tg_share, View.GONE)
            views.setTextViewText(R.id.bw_tg_main, context.getString(R.string.bw_start_room))
        }
        views.setOnClickPendingIntent(R.id.bw_tg_main, activityIntent(context, 91, openApp(context)))
        push(context, TogetherWidgetReceiver::class.java, views)
    }

    // ---- Resume ----
    fun renderResume(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.widget_bw_resume)
        val title = BaajaWidgetState.lastTitle(context)
        val artist = BaajaWidgetState.lastArtist(context)
        val dur = BaajaWidgetState.lastDur(context)
        val pos = BaajaWidgetState.lastPos(context)
        views.setTextViewText(R.id.bw_resume_title, title ?: context.getString(R.string.not_playing))
        views.setTextViewText(
            R.id.bw_resume_sub,
            if (title == null) context.getString(R.string.tap_to_play)
            else if (dur > 0 && !BaajaWidgetState.lastPlaying(context)) "${formatClock((dur - pos).coerceAtLeast(0))} · $artist" else artist,
        )
        views.setImageViewResource(R.id.bw_resume_play_icon, if (BaajaWidgetState.lastPlaying(context)) R.drawable.ic_bw_pause_on else R.drawable.ic_bw_play_on)
        val cover = BaajaWidgetState.coverFile(context, "last")
        if (cover.exists()) BitmapFactory.decodeFile(cover.path)?.let { views.setImageViewBitmap(R.id.bw_resume_art, it) }
        views.setOnClickPendingIntent(R.id.bw_resume_play, serviceIntent(context, 95, MusicWidgetReceiver.ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.bw_resume_art, activityIntent(context, 96, openApp(context)))
        push(context, ResumeWidgetReceiver::class.java, views)
    }
}
