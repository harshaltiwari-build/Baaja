/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.sponsorblock

import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.metrolist.music.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Skips SponsorBlock segments in the current track. Skips are scheduled as player messages at each
 * segment's start, so nothing polls the playback position.
 */
@UnstableApi
class SponsorBlockSkipper(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var enabled = false
        @MainThread set(value) {
            field = value
            if (!value) clear()
        }

    private val cache = HashMap<String, List<SkipSegment>>()
    private var fetchJob: Job? = null
    private val scheduled = mutableListOf<PlayerMessage>()

    @MainThread
    fun onTrackChanged(player: ExoPlayer, mediaId: String?) {
        clear()
        if (!enabled || mediaId == null) return

        cache[mediaId]?.let {
            schedule(player, mediaId, it)
            return
        }
        fetchJob = scope.launch {
            val segments =
                withContext(Dispatchers.IO) {
                    runCatching { SponsorBlockClient.segments(mediaId) }
                        .onFailure { Timber.tag(TAG).w(it, "Couldn't load segments for $mediaId") }
                        .getOrDefault(emptyList())
                }
            withContext(Dispatchers.Main) {
                cache[mediaId] = segments
                if (enabled && player.currentMediaItem?.mediaId == mediaId) schedule(player, mediaId, segments)
            }
        }
    }

    @MainThread
    fun clear() {
        fetchJob?.cancel()
        fetchJob = null
        scheduled.forEach { it.cancel() }
        scheduled.clear()
    }

    private fun schedule(player: ExoPlayer, mediaId: String, segments: List<SkipSegment>) {
        segments.forEach { segment ->
            scheduled +=
                player
                    .createMessage { _, _ ->
                        if (player.currentMediaItem?.mediaId == mediaId && player.currentPosition < segment.endMs - 250) {
                            player.seekTo(segment.endMs)
                            Toast.makeText(context, R.string.sponsorblock_skipped, Toast.LENGTH_SHORT).show()
                        }
                    }.setLooper(Looper.getMainLooper())
                    .setPosition(segment.startMs)
                    .setDeleteAfterDelivery(false)
                    .send()
        }
        if (segments.isNotEmpty()) Timber.tag(TAG).d("Scheduled ${segments.size} skips for $mediaId")
    }

    private companion object {
        const val TAG = "SponsorBlock"
    }
}
