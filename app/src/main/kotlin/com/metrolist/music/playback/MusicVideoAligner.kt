/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.ConnectivityManager
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.utils.InnerTubeXPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Finds where a song starts inside its music video when YouTube doesn't say (signed out), by matching
 * the rhythm of the song's opening against the video's own soundtrack. Music videos often open with a
 * scene before the music, so playing both from 0:00 leaves the picture far behind the song.
 */
object MusicVideoAligner {
    private const val TAG = "MusicVideoAligner"
    private const val HOP_MS = 20
    private const val SONG_FROM_MS = 5_000L
    private const val SONG_WINDOW_MS = 30_000L
    private const val MAX_INTRO_MS = 150_000L
    private const val MIN_MATCH = 0.35

    /** The video position where the song's 0:00 is, or null when it can't be told with confidence. */
    suspend fun introOffsetMs(
        songId: String,
        videoId: String,
        songDurationMs: Long,
        videoDurationMs: Long,
        cacheDir: File,
        connectivityManager: ConnectivityManager,
    ): Long? =
        withContext(Dispatchers.IO) {
            val extra = videoDurationMs - songDurationMs
            // No room for an intro: the two already line up from the start.
            if (songDurationMs <= 0 || videoDurationMs <= 0 || extra < 2_000) return@withContext 0L
            val maxLagMs = minOf(extra + 10_000, MAX_INTRO_MS)
            val dir = File(cacheDir, "video_align").apply { mkdirs() }
            val songFile = File(dir, "$songId.audio")
            val videoFile = File(dir, "$videoId.audio")
            try {
                coroutineScope {
                    val song = async { download(songId, SONG_FROM_MS + SONG_WINDOW_MS, songFile, connectivityManager) }
                    val video = async { download(videoId, SONG_FROM_MS + SONG_WINDOW_MS + maxLagMs, videoFile, connectivityManager) }
                    song.await()
                    video.await()
                }
                val songEnvelope = onsets(decodeEnergy(songFile, SONG_FROM_MS + SONG_WINDOW_MS))
                val videoEnvelope = onsets(decodeEnergy(videoFile, SONG_FROM_MS + SONG_WINDOW_MS + maxLagMs))
                ensureActive()
                bestLag(songEnvelope, videoEnvelope, (maxLagMs / HOP_MS).toInt())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Couldn't line up %s with %s", songId, videoId)
                null
            } finally {
                songFile.delete()
                videoFile.delete()
            }
        }

    /** Downloads about [ms] of a track's lowest-quality audio. */
    private suspend fun download(
        id: String,
        ms: Long,
        target: File,
        connectivityManager: ConnectivityManager,
    ) {
        val data =
            InnerTubeXPlayer
                .playerResponseForPlayback(id, audioQuality = AudioQuality.LOW, connectivityManager = connectivityManager)
                .getOrThrow()
        val bytesPerSecond = data.format.bitrate.coerceAtLeast(32_000) / 8L
        var wanted = bytesPerSecond * ms / 1000 * 5 / 4 + 64 * 1024
        data.format.contentLength?.let { wanted = minOf(wanted, it) }
        val chunk = if (data.useRangeChunks || data.requireBoundedRange) data.rangeChunkSizeBytes.coerceAtLeast(64 * 1024) else wanted
        target.outputStream().use { out ->
            var position = 0L
            while (position < wanted) {
                val end = minOf(position + chunk, wanted) - 1
                val connection = URL(data.streamUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    data.streamHeaders.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                    connection.setRequestProperty("Range", "bytes=$position-$end")
                    check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
                    val copied = connection.inputStream.use { it.copyTo(out) }
                    if (copied <= 0) break
                    position += copied
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    /** Loudness (mean square) of each 20 ms of the first [maxMs] of audio, mixed to mono. */
    private fun decodeEnergy(
        file: File,
        maxMs: Long,
    ): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val energy = ArrayList<Float>((maxMs / HOP_MS).toInt() + 1)
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).first { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!).apply {
                configure(format, null, null, 0)
                start()
            }
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var hopFrames = sampleRate * HOP_MS / 1000
            var sum = 0.0
            var frames = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var lastSampleRead = false
            while (energy.size * HOP_MS < maxMs) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(5_000)
                    if (index >= 0) {
                        // The download is cut short, so reading can fail where it ends: that's the end too.
                        val size = if (lastSampleRead) -1 else runCatching { extractor.readSampleData(codec.getInputBuffer(index)!!, 0) }.getOrDefault(-1)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime.coerceAtLeast(0), 0)
                            lastSampleRead = !extractor.advance()
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, 5_000)
                when {
                    out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        sampleRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        hopFrames = sampleRate * HOP_MS / 1000
                    }
                    out >= 0 -> {
                        val samples = codec.getOutputBuffer(out)!!.order(ByteOrder.nativeOrder()).asShortBuffer()
                        samples.position(info.offset / 2)
                        samples.limit((info.offset + info.size) / 2)
                        while (samples.remaining() >= channels) {
                            var mono = 0.0
                            repeat(channels) { mono += samples.get() }
                            mono /= channels * 32768.0
                            sum += mono * mono
                            if (++frames == hopFrames) {
                                energy += (sum / frames).toFloat()
                                sum = 0.0
                                frames = 0
                            }
                        }
                        codec.releaseOutputBuffer(out, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
        return energy.toFloatArray()
    }

    /** Where the sound gets louder: beats and note starts, which line up across both recordings. */
    private fun onsets(energy: FloatArray): FloatArray {
        val log = FloatArray(energy.size) { ln(energy[it] + 1e-6f) }
        return FloatArray(energy.size) { i -> if (i == 0) 0f else (log[i] - log[i - 1]).coerceAtLeast(0f) }
    }

    /** The shift of the video that best matches the song's opening, in milliseconds. */
    private fun bestLag(
        song: FloatArray,
        video: FloatArray,
        maxLag: Int,
    ): Long? {
        val from = (SONG_FROM_MS / HOP_MS).toInt()
        val length = minOf((SONG_WINDOW_MS / HOP_MS).toInt(), song.size - from)
        if (length < 500) return null
        val pattern = normalized(song, from, length)
        var best = -1.0
        var bestLag = 0
        for (lag in 0..maxLag) {
            if (from + lag + length > video.size) break
            var mean = 0.0
            for (i in 0 until length) mean += video[from + lag + i]
            mean /= length
            var dot = 0.0
            var norm = 0.0
            for (i in 0 until length) {
                val v = video[from + lag + i] - mean
                dot += pattern[i] * v
                norm += v * v
            }
            if (norm <= 0) continue
            val score = dot / sqrt(norm)
            if (score > best) {
                best = score
                bestLag = lag
            }
        }
        Timber.tag(TAG).d("Best match %.2f at %d ms", best, bestLag.toLong() * HOP_MS)
        return if (best >= MIN_MATCH) bestLag.toLong() * HOP_MS else null
    }

    private fun normalized(
        values: FloatArray,
        from: Int,
        length: Int,
    ): DoubleArray {
        var mean = 0.0
        for (i in 0 until length) mean += values[from + i]
        mean /= length
        val out = DoubleArray(length) { values[from + it] - mean }
        val norm = sqrt(out.sumOf { it * it })
        if (norm > 0) for (i in out.indices) out[i] /= norm
        return out
    }
}
