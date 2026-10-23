/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.metrolist.music.constants.SmartDownloadsCountKey
import com.metrolist.music.constants.SmartDownloadsDismissedKey
import com.metrolist.music.constants.SmartDownloadsLastRunKey
import com.metrolist.music.constants.SmartDownloadsTrackedKey
import com.metrolist.music.constants.SmartDownloadsWifiOnlyKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Choices offered in settings. 0 = off. */
val SmartDownloadsCounts = listOf(0, 10, 25, 50, 100)

/** What one Smart downloads run should do. */
data class SmartDownloadsPlan(
    val toDownload: List<String>,
    val toRemove: List<String>,
    /** Ids Smart downloads owns after this run. */
    val tracked: Set<String>,
    val dismissed: Set<String>,
)

/**
 * Pure top-N diff.
 *
 * @param ranked most-played song ids, best first (may be longer than [limit]).
 * @param tracked ids this feature added earlier. Nothing outside this set is ever removed, so
 *   songs the user downloaded by hand are safe.
 * @param present ids that currently have a download (any state except being removed).
 * @param failed subset of [present] whose download failed; retried if still wanted.
 * @param dismissed ids the user deleted after we added them; never re-added.
 */
fun planSmartDownloads(
    ranked: List<String>,
    limit: Int,
    tracked: Set<String>,
    present: Set<String>,
    failed: Set<String>,
    dismissed: Set<String>,
): SmartDownloadsPlan {
    // A tracked song that has no download any more was deleted by the user: respect that.
    val userRemoved = tracked - present
    val allDismissed = dismissed + userRemoved
    val stillTracked = tracked - userRemoved

    val wanted = ranked.asSequence().distinct().filterNot { it in allDismissed }.take(limit.coerceAtLeast(0)).toList()
    val wantedSet = wanted.toSet()

    val newOnes = wanted.filter { it !in present }
    val retries = wanted.filter { it in failed && it in stillTracked }
    val toRemove = stillTracked.filter { it !in wantedSet }

    return SmartDownloadsPlan(
        toDownload = newOnes + retries,
        toRemove = toRemove,
        tracked = (stillTracked - toRemove.toSet()) + newOnes,
        // Forget dismissals once a song falls far out of the ranking so the set can't grow forever.
        dismissed = allDismissed.filterTo(mutableSetOf()) { it in ranked },
    )
}

/** Schedules the daily Smart downloads sync. */
object SmartDownloads {
    private const val PERIODIC_WORK = "baaja_smart_downloads"
    private const val NOW_WORK = "baaja_smart_downloads_now"

    fun schedule(context: Context, count: Int, wifiOnly: Boolean, runNow: Boolean = true) {
        val workManager = WorkManager.getInstance(context)
        if (count <= 0) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            workManager.cancelUniqueWork(NOW_WORK)
            return
        }
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SmartDownloadsWorker>(1, TimeUnit.DAYS).setConstraints(constraints).build(),
        )
        if (runNow) {
            workManager.enqueueUniqueWork(
                NOW_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SmartDownloadsWorker>().setConstraints(constraints).build(),
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmartDownloadsEntryPoint {
    fun database(): MusicDatabase

    fun downloadUtil(): DownloadUtil
}

@UnstableApi
class SmartDownloadsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.dataStore.data.first()
        val limit = prefs[SmartDownloadsCountKey] ?: 0
        if (limit <= 0) return Result.success()
        val wifiOnly = prefs[SmartDownloadsWifiOnlyKey] ?: true

        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, SmartDownloadsEntryPoint::class.java)
        val database = entryPoint.database()
        val downloadUtil = entryPoint.downloadUtil()
        val manager = downloadUtil.downloadManager

        val ranked =
            database
                .mostPlayedSongs(LocalDateTime.now().minusDays(RANKING_WINDOW_DAYS), limit = limit * 2 + 20)
                .first()
                .filter { !it.song.isLocal && !it.song.isEpisode }
                .map { it.id }
        val downloads = downloadUtil.downloads.value.values.filter { it.state != Download.STATE_REMOVING }
        val plan =
            planSmartDownloads(
                ranked = ranked,
                limit = limit,
                tracked = prefs[SmartDownloadsTrackedKey].orEmpty(),
                present = downloads.mapTo(mutableSetOf()) { it.request.id },
                failed = downloads.filter { it.state == Download.STATE_FAILED }.mapTo(mutableSetOf()) { it.request.id },
                dismissed = prefs[SmartDownloadsDismissedKey].orEmpty(),
            )
        Timber.tag(TAG).i("Smart downloads: +%d -%d (keeping %d)", plan.toDownload.size, plan.toRemove.size, plan.tracked.size)

        // Persist ownership before touching downloads, so a crash can't orphan songs we added.
        applicationContext.safeDataStoreEdit {
            it[SmartDownloadsTrackedKey] = plan.tracked
            it[SmartDownloadsDismissedKey] = plan.dismissed
        }

        val titles = plan.toDownload.associateWith { id -> database.songEntity(id)?.title ?: id }

        // DownloadManager is driven from the main thread; DownloadService can't be started from
        // the background on Android 12+, so talk to the manager directly while this worker runs.
        withContext(Dispatchers.Main) {
            plan.toRemove.forEach(manager::removeDownload)
            plan.tracked.forEach { id -> manager.setStopReason(id, Download.STOP_REASON_NONE) }
            plan.toDownload.forEach { id ->
                val title = titles[id] ?: id
                manager.addDownload(
                    DownloadRequest.Builder(id, id.toUri())
                        .setCustomCacheKey(id)
                        .setData(title.toByteArray())
                        .build(),
                )
            }
            runCatching { DownloadService.startForeground(applicationContext, ExoDownloadService::class.java) }
                .onFailure { Timber.tag(TAG).d("Download service not started from background: %s", it.message) }
        }

        val pending = plan.tracked
        try {
            withTimeoutOrNull(MAX_WAIT_MS) {
                downloadUtil.downloads.first { map ->
                    pending.all { id -> map[id]?.state.let { it == null || it in FINISHED_STATES } }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                // Leaving Wi-Fi (or timing out) must not let our downloads continue on mobile data.
                if (wifiOnly) {
                    pending
                        .filter { id -> downloadUtil.downloads.value[id]?.state.let { it == Download.STATE_QUEUED || it == Download.STATE_DOWNLOADING } }
                        .forEach { id -> manager.setStopReason(id, STOP_REASON_SMART_DOWNLOADS) }
                }
            }
        }

        applicationContext.safeDataStoreEdit { it[SmartDownloadsLastRunKey] = System.currentTimeMillis() }
        return Result.success()
    }

    private companion object {
        const val TAG = "SmartDownloads"
        const val RANKING_WINDOW_DAYS = 90L
        const val MAX_WAIT_MS = 9 * 60 * 1000L
        const val STOP_REASON_SMART_DOWNLOADS = 0x5D
        val FINISHED_STATES = setOf(Download.STATE_COMPLETED, Download.STATE_FAILED, Download.STATE_STOPPED)
    }
}
