/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Metrolist (MetrolistGroup/metrolist, GPL-3.0).
 */

package com.metrolist.music.playback.sabr

interface SeekableMediaSource {
    val mediaSize: Long?
    val mediaPosition: Long
    val bufferedBytePosition: Long get() = mediaPosition

    fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int

    fun readAtMediaPosition(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int

    fun seekToMediaPosition(position: Long): Long

    fun updatePlaybackPosition(positionMs: Long)

    fun close()
}
