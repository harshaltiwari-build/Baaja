/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.automix

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import timber.log.Timber
import java.io.IOException
import java.nio.ByteOrder

/**
 * Decodes a time range of a song straight from the player/download caches (never the network)
 * and returns its loudness envelope. Returns null when the range isn't cached yet or the
 * container can't be read; callers then fall back to the plain crossfade.
 */
@UnstableApi
class CachedAudioScanner(private val caches: List<Cache>) {
    fun scan(
        mediaId: String,
        fromMs: Long,
        toMs: Long,
        deadlineNanos: Long,
    ): LoudnessEnvelope? {
        for (cache in caches) {
            val length = ContentMetadata.getContentLength(cache.getContentMetadata(mediaId))
            if (length <= 0) continue
            val envelope =
                try {
                    decode(CacheMediaDataSource(cache, mediaId, length), fromMs, toMs, deadlineNanos)
                } catch (e: Exception) {
                    Timber.tag(TAG).d("Scan of %s failed in one cache: %s", mediaId, e.message)
                    null
                }
            if (envelope != null && envelope.levelsDb.isNotEmpty()) return envelope
        }
        return null
    }

    private fun decode(
        source: MediaDataSource,
        fromMs: Long,
        toMs: Long,
        deadlineNanos: Long,
    ): LoudnessEnvelope? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            if (fromMs > 0) extractor.seekTo(fromMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var isFloat = false
            var builder = LoudnessEnvelopeBuilder(sampleRate, channels, fromMs)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            val endUs = toMs * 1000

            while (System.nanoTime() < deadlineNanos) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0 || extractor.sampleTime > endUs) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = codec.outputFormat
                        sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        isFloat = out.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            out.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        if (builder.windowCount == 0) builder = LoudnessEnvelopeBuilder(sampleRate, channels, fromMs)
                    }
                    outIndex >= 0 -> {
                        val buffer = codec.getOutputBuffer(outIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            buffer.order(ByteOrder.nativeOrder())
                            val frameUs = 1_000_000.0 / sampleRate
                            var sampleIndex = 0
                            if (isFloat) {
                                while (buffer.remaining() >= 4) {
                                    builder.add(buffer.getFloat(), info.presentationTimeUs + ((sampleIndex++ / channels) * frameUs).toLong())
                                }
                            } else {
                                while (buffer.remaining() >= 2) {
                                    builder.add(buffer.getShort() / 32768f, info.presentationTimeUs + ((sampleIndex++ / channels) * frameUs).toLong())
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || info.presentationTimeUs > endUs) {
                            return builder.build()
                        }
                    }
                }
            }
            Timber.tag(TAG).d("Scan ran out of time")
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
            runCatching { source.close() }
        }
    }

    /** Random-access reads over one cached resource; uncached ranges throw instead of fetching. */
    private class CacheMediaDataSource(
        cache: Cache,
        private val key: String,
        private val length: Long,
    ) : MediaDataSource() {
        private val factory = CacheDataSource.Factory().setCache(cache).setCacheWriteDataSinkFactory(null)
        private var source: DataSource? = null
        private var position = -1L

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= length) return -1
            if (size == 0) return 0
            if (source == null || position != this.position) {
                source?.close()
                val dataSource = factory.createDataSource()
                dataSource.open(
                    DataSpec.Builder()
                        .setUri(key.toUri())
                        .setKey(key)
                        .setPosition(position)
                        .build(),
                )
                source = dataSource
                this.position = position
            }
            val read = source!!.read(buffer, offset, size)
            if (read == C.RESULT_END_OF_INPUT) return -1
            this.position += read
            return read
        }

        override fun getSize(): Long = length

        override fun close() {
            try {
                source?.close()
            } catch (_: IOException) {
            }
            source = null
            position = -1L
        }
    }

    private companion object {
        const val TAG = "AutomixScanner"
        const val TIMEOUT_US = 10_000L
    }
}
