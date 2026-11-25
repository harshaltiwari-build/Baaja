/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.utils.backToMain

/**
 * Downloads that aren't finished yet: queued, in progress, paused or failed, with pause/resume for all
 * and retry or cancel per song. Finished downloads stay in the Downloaded playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsManagerScreen(navController: NavController) {
    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val downloads by downloadUtil.downloads.collectAsStateWithLifecycle()
    val active =
        remember(downloads) {
            downloads.values
                .filter { it.state != Download.STATE_COMPLETED && it.state != Download.STATE_REMOVING }
                .sortedWith(compareBy({ it.state.order() }, { it.startTimeMs }))
        }
    val completedCount = remember(downloads) { downloads.values.count { it.state == Download.STATE_COMPLETED } }
    var songs by remember { mutableStateOf<Map<String, Song>>(emptyMap()) }
    LaunchedEffect(active.map { it.request.id }) {
        val missing = active.map { it.request.id }.filter { it !in songs }
        if (missing.isNotEmpty()) songs = songs + database.getSongsByIds(missing).associateBy { it.id }
    }
    val allPaused by downloadUtil.downloadsPaused.collectAsStateWithLifecycle()
    // DownloadManager only reports progress when a download changes state, so read it every second
    // while something is downloading and this screen is visible.
    var progress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    val anyDownloading = active.any { it.state == Download.STATE_DOWNLOADING }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(anyDownloading, lifecycleOwner) {
        if (!anyDownloading) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                progress = downloadUtil.downloadManager.currentDownloads.associate { it.request.id to it.percentDownloaded }
                delay(1_000)
            }
        }
    }
    val insets = LocalPlayerAwareWindowInsets.current

    LazyColumn(
        contentPadding = insets.asPaddingValues(),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "summary") {
            BaajaCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (active.isEmpty()) {
                            stringResource(R.string.baaja_downloads_none_active)
                        } else {
                            pluralStringResource(R.plurals.baaja_downloads_in_progress, active.size, active.size)
                        },
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        pluralStringResource(R.plurals.baaja_downloads_completed, completedCount, completedCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (active.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BaajaPillButton(
                                text = stringResource(if (allPaused) R.string.baaja_downloads_resume_all else R.string.baaja_downloads_pause_all),
                                onClick = { if (allPaused) downloadUtil.resumeAll() else downloadUtil.pauseAll() },
                                icon = if (allPaused) R.drawable.play else R.drawable.pause,
                                height = 46.dp,
                                modifier = Modifier.weight(1f),
                            )
                            BaajaPillButton(
                                text = stringResource(R.string.baaja_downloads_cancel_all),
                                onClick = { downloadUtil.cancelUnfinished(active.map { it.request.id }) },
                                primary = false,
                                icon = R.drawable.close,
                                height = 46.dp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        items(active, key = { it.request.id }) { download ->
            DownloadRow(
                download = download,
                percent = progress[download.request.id] ?: download.percentDownloaded,
                song = songs[download.request.id],
                paused = allPaused,
                onRetry = { downloadUtil.retry(download.request.id) },
                onCancel = { downloadUtil.remove(download.request.id) },
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.baaja_downloads_title)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

/** Failed first (they need attention), then downloading, then waiting. */
private fun Int.order(): Int =
    when (this) {
        Download.STATE_FAILED -> 0
        Download.STATE_DOWNLOADING -> 1
        Download.STATE_RESTARTING -> 2
        else -> 3
    }

@Composable
private fun DownloadRow(
    download: Download,
    percent: Float,
    song: Song?,
    paused: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val title = song?.song?.title ?: String(download.request.data)
    val failed = download.state == Download.STATE_FAILED
    val status =
        when {
            failed -> stringResource(R.string.baaja_downloads_failed)
            download.state == Download.STATE_DOWNLOADING && !paused ->
                stringResource(R.string.baaja_downloads_percent, percent.coerceIn(0f, 100f).toInt())
            paused || download.stopReason != Download.STOP_REASON_NONE -> stringResource(R.string.baaja_downloads_paused)
            else -> stringResource(R.string.baaja_downloads_waiting)
        }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BaajaArtwork(song?.song?.thumbnailUrl, Modifier.size(52.dp), shape = RoundedCornerShape(12.dp), shadowOffset = 0.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(song?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }, status).joinToString(" · "),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (download.state == Download.STATE_DOWNLOADING && !paused) {
                LinearProgressIndicator(
                    progress = { percent.coerceIn(0f, 100f) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (failed) {
            androidx.compose.material3.IconButton(onClick = onRetry) {
                Icon(painterResource(R.drawable.refresh), contentDescription = stringResource(R.string.baaja_downloads_retry))
            }
        }
        androidx.compose.material3.IconButton(onClick = onCancel) {
            Icon(painterResource(R.drawable.close), contentDescription = stringResource(R.string.baaja_downloads_cancel))
        }
    }
}
