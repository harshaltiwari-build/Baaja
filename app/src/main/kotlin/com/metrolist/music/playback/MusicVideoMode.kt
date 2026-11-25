/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.net.ConnectivityManager
import android.view.TextureView
import androidx.core.content.getSystemService
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.metrolist.innertube.pages.MusicVideoCounterpart
import com.metrolist.innertubex.sabr.SabrVideoStream
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.MusicVideosEnabledKey
import com.metrolist.music.constants.MusicVideoQualityKey
import com.metrolist.music.extensions.metadata
import com.metrolist.music.playback.sabr.SabrInputStream
import com.metrolist.music.playback.sabr.SyncedVideoPlayer
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.InnerTubeXPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Optional
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Shows the playing song's music video in place of its artwork. The song keeps playing as usual; the
 * video is silent and follows the song's position, so all the player controls keep working. While
 * Video is selected, each new song brings up its own video.
 */
@UnstableApi
object MusicVideoMode {
    sealed interface State {
        data object Off : State

        data object Loading : State

        data object Playing : State

        data object NoVideo : State

        data object Failed : State
    }

    private const val TAG = "MusicVideoMode"
    private const val MAX_HEIGHT = 720

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Width / height of the video, for sizing the picture. */
    val aspectRatio = MutableStateFlow(16f / 9f)

    val isActive: Boolean get() = _state.value != State.Off

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appContext: Context? = null
    private var musicPlayer: Player? = null
    private var videoPlayer: SyncedVideoPlayer? = null
    private var loadJob: Job? = null
    private var loadedFor: String? = null
    private var counterpart: MusicVideoCounterpart? = null
    private var surface: TextureView? = null

    private val listener =
        object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                if (mediaItem != null && mediaItem.mediaId != loadedFor) loadFor(mediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                videoPlayer?.setPlaying(musicPlayer?.playWhenReady == true)
            }

            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                videoPlayer?.setPlaying(playWhenReady)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) videoPlayer?.resync()
            }
        }

    fun start(
        context: Context,
        player: Player,
    ) {
        if (isActive) return
        appContext = context.applicationContext
        musicPlayer = player
        player.addListener(listener)
        player.currentMediaItem?.let(::loadFor) ?: run { _state.value = State.NoVideo }
    }

    fun stop() {
        loadJob?.cancel()
        musicPlayer?.removeListener(listener)
        musicPlayer = null
        videoPlayer?.close()
        videoPlayer = null
        loadedFor = null
        counterpart = null
        _state.value = State.Off
    }

    private val surfaces = mutableListOf<TextureView>()
    private var pipSurface: TextureView? = null

    /**
     * Views showing the video: the Picture-in-Picture window wins while it's open (the player card behind
     * it may rebuild when the window shrinks); otherwise the newest view gets the picture.
     */
    fun attachSurface(
        view: TextureView,
        pictureInPicture: Boolean = false,
    ) {
        if (pictureInPicture) {
            pipSurface = view
        } else {
            surfaces.remove(view)
            surfaces += view
        }
        applySurface()
    }

    fun detachSurface(view: TextureView) {
        if (pipSurface === view) pipSurface = null
        surfaces.remove(view)
        applySurface()
    }

    private fun applySurface() {
        surface = pipSurface ?: surfaces.lastOrNull()
        videoPlayer?.setSurface(surface)
    }

    /** Where the video should be for the song's current position. */
    private fun videoPosition(): Long {
        val songPosition = musicPlayer?.currentPosition ?: 0L
        return counterpart?.toTarget(songPosition) ?: songPosition
    }

    /** A song's official video: [counterpart] is null when the song is itself the video. */
    private class VideoMatch(
        val counterpart: MusicVideoCounterpart?,
    )

    private val lookups = java.util.concurrent.ConcurrentHashMap<String, Optional<VideoMatch>>()
    private val pendingLookups = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<VideoMatch?>>()

    private val _hasVideo = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    /** Which songs have an official music video, as far as checked; the Song | Video switch only shows for these. */
    val hasVideo: StateFlow<Map<String, Boolean>> = _hasVideo.asStateFlow()

    /** Looks up (once per song) whether it has an official music video, in the background. */
    fun check(item: MediaItem) {
        if (lookups.containsKey(item.mediaId) || pendingLookups.containsKey(item.mediaId)) return
        scope.launch { runCatching { findVideo(item) } }
    }

    /**
     * Called when a song starts: checks for its video straight away (unless videos are switched off),
     * so the answer is ready before the player is opened and the switch doesn't pop in late.
     */
    fun prefetch(
        context: Context,
        item: MediaItem,
    ) {
        if (lookups.containsKey(item.mediaId) || pendingLookups.containsKey(item.mediaId)) return
        scope.launch(Dispatchers.IO) {
            val enabled = context.dataStore.get(MusicVideosEnabledKey, true) && !context.dataStore.get(HideVideoSongsKey, false)
            if (enabled) runCatching { findVideo(item) }
        }
    }

    private suspend fun findVideo(item: MediaItem): VideoMatch? {
        lookups[item.mediaId]?.let { return it.orElse(null) }
        val pending =
            pendingLookups.getOrPut(item.mediaId) {
                scope.async(Dispatchers.IO) {
                    val metadata = item.metadata
                    var lookupFailed = false
                    val match =
                        if (metadata?.musicVideoType?.let { it != MUSIC_VIDEO_TYPE_ATV } == true) {
                            VideoMatch(null)
                        } else {
                            val paired = YouTube.musicVideoCounterpart(item.mediaId)
                            if (paired.isFailure) lookupFailed = true
                            (
                                paired.getOrNull()
                                    ?: searchOfficialVideo(item.mediaId, metadata?.title, metadata?.artists?.firstOrNull()?.name) { lookupFailed = true }
                            )?.let(::VideoMatch)
                        }
                    // A network failure isn't "no video": leave it unanswered so the next check tries again.
                    if (match != null || !lookupFailed) {
                        lookups[item.mediaId] = Optional.ofNullable(match)
                        _hasVideo.value = _hasVideo.value + (item.mediaId to (match != null))
                    }
                    match
                }
            }
        return try {
            pending.await()
        } finally {
            pendingLookups.remove(item.mediaId, pending)
        }
    }

    private fun loadFor(item: MediaItem) {
        val context = appContext ?: return
        loadJob?.cancel()
        videoPlayer?.close()
        videoPlayer = null
        loadedFor = item.mediaId
        _state.value = State.Loading
        val sourceId = item.mediaId

        loadJob =
            scope.launch {
                try {
                    val found = findVideo(item)
                    if (found == null) {
                        _state.value = State.NoVideo
                        return@launch
                    }
                    counterpart = found.counterpart
                    val target = counterpart?.targetVideoId ?: sourceId
                    val source =
                        withContext(Dispatchers.IO) {
                            InnerTubeXPlayer.videoSourceForPlayback(
                                target,
                                context.dataStore.get(MusicVideoQualityKey, MAX_HEIGHT),
                                context.getSystemService<ConnectivityManager>()!!,
                            )
                        }.getOrThrow()
                    counterpart = counterpart?.let { aligned(it, item, source.durationMs, context) }
                    val player =
                        SyncedVideoPlayer(
                            context = context,
                            onVideoSize = { aspectRatio.value = it },
                            onFirstFrame = { _state.value = State.Playing },
                            onError = { error ->
                                Timber.tag(TAG).w(error, "Music video playback failed")
                                _state.value = State.Failed
                            },
                        )
                    videoPlayer = player
                    player.setSurface(surface)
                    val sabr = source.sabr
                    if (sabr != null) {
                        val stream =
                            SabrInputStream(
                                mediaSize = source.contentLengthBytes,
                                streamFactory = { initial, position ->
                                    SabrVideoStream(
                                        httpClient = InnerTubeXPlayer.sabrHttpClient(),
                                        bootstrap = sabr,
                                        playbackPositionMs = position,
                                        initialPlayerTimeMs = initial,
                                        logger = InnerTubeXPlayer.sabrLogger,
                                    ).chunks()
                                },
                                tempDirectory = context.cacheDir,
                            )
                        player.loadSabr(stream, ::videoPosition)
                    } else {
                        player.loadDirect(source.directUrl!!, source.headers, ::videoPosition)
                    }
                    player.setPlaying(musicPlayer?.playWhenReady == true)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Couldn't load the music video")
                    _state.value = State.Failed
                }
            }
    }

    private val introOffsets = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * A video found by search comes without timing, and music videos often open with a scene before
     * the song: find where the song starts in it so the picture doesn't trail the music.
     */
    private suspend fun aligned(
        counterpart: MusicVideoCounterpart,
        item: MediaItem,
        videoDurationMs: Long?,
        context: Context,
    ): MusicVideoCounterpart {
        if (counterpart.hasTiming) return counterpart
        val key = "${counterpart.sourceVideoId}>${counterpart.targetVideoId}"
        val offset =
            introOffsets[key] ?: run {
                val songDurationMs =
                    musicPlayer?.duration?.takeIf { it > 0 }
                        ?: item.metadata?.duration?.takeIf { it > 0 }?.times(1000L)
                        ?: return counterpart
                MusicVideoAligner
                    .introOffsetMs(
                        songId = counterpart.sourceVideoId,
                        videoId = counterpart.targetVideoId,
                        songDurationMs = songDurationMs,
                        videoDurationMs = videoDurationMs ?: return counterpart,
                        cacheDir = context.cacheDir,
                        connectivityManager = context.getSystemService<ConnectivityManager>()!!,
                    )?.also { introOffsets[key] = it } ?: return counterpart
            }
        if (offset == 0L) return counterpart
        return MusicVideoCounterpart(
            counterpart.sourceVideoId,
            counterpart.targetVideoId,
            listOf(MusicVideoCounterpart.Segment(primaryStartMs = 0, counterpartStartMs = offset, durationMs = Long.MAX_VALUE / 4)),
            forward = true,
        )
    }

    /**
     * Signed out, YouTube doesn't say which video belongs to a song, so search its videos and accept
     * only an official music video of this exact song: never lyric videos, covers, remixes or uploads
     * by fans. Without one, the player says the song has no music video.
     */
    private suspend fun searchOfficialVideo(
        sourceId: String,
        title: String?,
        artist: String?,
        onFailure: () -> Unit,
    ): MusicVideoCounterpart? {
        if (title.isNullOrBlank()) return null
        val wanted = title.normalizedTitle()
        if (wanted.isEmpty()) return null
        val artistName = artist?.lowercase()?.trim()
        // Search results vary from one request to the next, so a second wording is tried before giving up.
        for (query in listOfNotNull(listOfNotNull(title, artist).joinToString(" "), "$title official video")) {
            val candidates =
                YouTube
                    .search(query, YouTube.SearchFilter.FILTER_VIDEO)
                    .onFailure { onFailure() }
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    .orEmpty()
                    .filter { video ->
                        video.id != sourceId &&
                            !video.isEpisode &&
                            (video.musicVideoType == MUSIC_VIDEO_TYPE_OMV || video.isFromOfficialLabel()) &&
                            wanted in video.title.normalizedTitle() &&
                            // Versions the song itself isn't: only allowed when the song's own title says so.
                            notOfficialMarkers.none { marker -> marker.containsWordIn(video.title) && !marker.containsWordIn(title) }
                    }
            // YouTube's own "official" tag first, then the artist's channel, then a label's channel.
            val match =
                candidates.firstOrNull { it.musicVideoType == MUSIC_VIDEO_TYPE_OMV && artistName != null && it.artists.any { a -> a.name.lowercase().contains(artistName) } }
                    ?: candidates.firstOrNull { it.musicVideoType == MUSIC_VIDEO_TYPE_OMV }
                    ?: candidates.firstOrNull()
            if (match != null) return MusicVideoCounterpart(sourceId, match.id, emptyList(), forward = true)
        }
        return null
    }

    /** Uploaded by a record label's own channel: their uploads are the real videos even when YouTube doesn't tag them. */
    private fun SongItem.isFromOfficialLabel(): Boolean {
        val channel = artists.firstOrNull()?.name?.lowercase()?.trim() ?: return false
        return channel in officialLabelChannels || channel.endsWith("vevo")
    }

    private val officialLabelChannels =
        setOf(
            "t-series", "sony music india", "zee music company", "tips official", "tips music", "saregama music",
            "yrf", "eros now music", "speed records", "times music", "white hill music", "desi melodies",
            "venus", "shemaroo filmi gaane", "aditya music", "lahari music", "think music india", "sony music south",
            "t-series tamil", "t-series telugu", "saregama tamil", "saregama telugu", "universal music india",
            "warner music india", "coke studio bharat", "coke studio pakistan",
        )

    private val notOfficialMarkers =
        listOf("lyric", "lyrics", "lyrical", "audio", "visualizer", "visualiser", "cover", "remix", "slowed", "reverb", "8d", "karaoke", "instrumental", "live", "reaction", "teaser", "jukebox", "status", "shorts")

    private fun String.containsWordIn(text: String): Boolean = Regex("""\b${Regex.escape(this)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)

    /** The song's name without "(From …)", "(feat. …)" and punctuation, for comparing titles. */
    private fun String.normalizedTitle(): String =
        lowercase()
            .replace(Regex("""\(.*?\)|\[.*?\]"""), " ")
            .replace(Regex("""[^\p{L}\p{N} ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
