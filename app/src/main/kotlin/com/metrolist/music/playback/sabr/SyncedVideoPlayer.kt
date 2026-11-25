/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Metrolist's VideoPlayer (MetrolistGroup/metrolist, GPL-3.0).
 */

package com.metrolist.music.playback.sabr

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Plays a music video's picture with no sound, kept in step with [positionProvider] (the song that's
 * playing): small drift is corrected by speeding up or slowing down slightly, large drift by seeking.
 */
@UnstableApi
class SyncedVideoPlayer(
    private val context: Context,
    private val onVideoSize: (Float) -> Unit,
    private val onFirstFrame: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var exoPlayer: ExoPlayer? = null
    private var seekableSource: SeekableMediaSource? = null
    private var syncJob: Job? = null
    private var positionProvider: () -> Long = { 0L }
    private var surfaceView: TextureView? = null
    private var syncSuppressedUntilNanos = 0L

    fun loadDirect(
        url: String,
        headers: Map<String, String>,
        positionProvider: () -> Long,
    ) = prepare(
        DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true),
        MediaItem.fromUri(url),
        null,
        positionProvider,
    )

    fun loadSabr(
        source: SeekableMediaSource,
        positionProvider: () -> Long,
    ) = prepare(DataSource.Factory { SeekableMediaDataSource(source) }, MediaItem.fromUri(SABR_VIDEO_URI), source, positionProvider)

    private fun prepare(
        dataSourceFactory: DataSource.Factory,
        mediaItem: MediaItem,
        source: SeekableMediaSource?,
        positionProvider: () -> Long,
    ) {
        this.positionProvider = positionProvider
        releasePlayer()
        seekableSource = source
        val trackSelector =
            DefaultTrackSelector(context).apply {
                parameters =
                    buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
            }
        val loadControl =
            DefaultLoadControl
                .Builder()
                .setBufferDurationsMs(20_000, 60_000, 2_000, 5_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(5_000, true)
                .build()
        exoPlayer =
            ExoPlayer
                .Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
                .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    volume = 0f
                    setMediaItem(mediaItem, positionProvider().coerceAtLeast(0))
                    addListener(
                        object : Player.Listener {
                            override fun onVideoSizeChanged(videoSize: VideoSize) {
                                if (videoSize.width > 0 && videoSize.height > 0) {
                                    onVideoSize(videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height)
                                }
                            }

                            override fun onRenderedFirstFrame() = onFirstFrame()

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) = onError(error)

                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                if (isPlaying) startSync()
                            }
                        },
                    )
                    surfaceView?.let(::setVideoTextureView)
                    prepare()
                }
    }

    fun setSurface(view: TextureView?) {
        surfaceView = view
        val player = exoPlayer ?: return
        if (view == null) player.clearVideoSurface() else player.setVideoTextureView(view)
    }

    /** Follows the song: plays while it plays, pauses while it's paused. */
    fun setPlaying(playing: Boolean) {
        val player = exoPlayer ?: return
        if (playing) {
            val target = positionProvider().coerceAtLeast(0)
            if (abs(player.currentPosition - target) > 1_000) player.seekTo(target)
            player.playWhenReady = true
            startSync()
        } else {
            player.playWhenReady = false
            syncJob?.cancel()
        }
    }

    /** After the song was seeked: jump straight there instead of drifting. */
    fun resync() {
        val player = exoPlayer ?: return
        val target = positionProvider().coerceAtLeast(0)
        (seekableSource as? PlaybackPositionInput)?.beginPlaybackSeek(target)
        player.seekTo(target)
        (seekableSource as? PlaybackPositionInput)?.endPlaybackSeek(target)
        syncSuppressedUntilNanos = System.nanoTime() + 500_000_000L
    }

    private fun startSync() {
        syncJob?.cancel()
        syncJob =
            scope.launch {
                var appliedSpeed = 1.0f
                while (isActive) {
                    val player = exoPlayer ?: break
                    if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) break
                    val master = positionProvider().coerceAtLeast(0)
                    seekableSource?.updatePlaybackPosition(master)
                    val now = System.nanoTime()
                    if (player.playbackState != Player.STATE_READY || !player.isPlaying || now < syncSuppressedUntilNanos) {
                        delay(50)
                        continue
                    }
                    val diff = master - player.currentPosition.coerceAtLeast(0)
                    val absDiff = abs(diff)
                    val targetSpeed =
                        when {
                            absDiff <= 15 -> 1.0f
                            absDiff <= 80 -> (1.0f + (diff / 500f).coerceIn(-0.08f, 0.08f)).let { (it * 100).roundToInt() / 100f }
                            absDiff <= 300 -> (1.0f + (diff / 600f).coerceIn(-0.25f, 0.25f)).let { (it * 100).roundToInt() / 100f }
                            absDiff <= 1_000 -> (1.0f + (diff / 800f).coerceIn(-0.5f, 0.5f)).let { (it * 100).roundToInt() / 100f }
                            else -> {
                                player.seekTo(master)
                                syncSuppressedUntilNanos = now + 500_000_000L
                                1.0f
                            }
                        }
                    if (abs(appliedSpeed - targetSpeed) >= 0.02f) {
                        appliedSpeed = targetSpeed
                        player.setPlaybackSpeed(targetSpeed)
                    }
                    delay(50)
                }
            }
    }

    private fun releasePlayer() {
        syncJob?.cancel()
        seekableSource?.close()
        seekableSource = null
        exoPlayer?.release()
        exoPlayer = null
    }

    fun close() {
        releasePlayer()
        scope.cancel()
    }

    /** Lets ExoPlayer read a SABR session as if it were one seekable file. */
    private class SeekableMediaDataSource(
        private val source: SeekableMediaSource,
    ) : BaseDataSource(false) {
        private var opened = false
        private var uri: Uri = Uri.EMPTY
        private var readPosition = 0L
        private var bytesRemaining = C.LENGTH_UNSET.toLong()

        override fun open(dataSpec: DataSpec): Long {
            uri = dataSpec.uri
            transferInitializing(dataSpec)
            source.seekToMediaPosition(dataSpec.position)
            readPosition = dataSpec.position
            val available = source.mediaSize?.minus(dataSpec.position)?.coerceAtLeast(0L)
            bytesRemaining =
                when {
                    dataSpec.length != C.LENGTH_UNSET.toLong() -> available?.let { minOf(dataSpec.length, it) } ?: dataSpec.length
                    available != null -> available
                    else -> C.LENGTH_UNSET.toLong()
                }
            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
            val requested = if (bytesRemaining == C.LENGTH_UNSET.toLong()) length else minOf(length.toLong(), bytesRemaining).toInt()
            val count = source.readAtMediaPosition(readPosition, buffer, offset, requested)
            if (count <= 0) return C.RESULT_END_OF_INPUT
            readPosition += count
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= count
            bytesTransferred(count)
            return count
        }

        override fun getUri(): Uri = uri

        override fun close() {
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    private companion object {
        val SABR_VIDEO_URI: Uri = "sabr-video://stream".toUri()
    }
}
