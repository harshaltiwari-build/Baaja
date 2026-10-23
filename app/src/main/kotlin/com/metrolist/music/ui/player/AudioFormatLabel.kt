/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import com.metrolist.music.db.entities.FormatEntity

/** Short "OPUS · 160 kbps" style label for the stream that is playing, or null if unknown. */
internal fun FormatEntity.audioFormatLabel(): String? {
    val codec =
        when {
            codecs.contains("opus", ignoreCase = true) -> "OPUS"
            codecs.startsWith("mp4a", ignoreCase = true) -> "AAC"
            codecs.isNotBlank() -> codecs.substringBefore('.').uppercase()
            else -> mimeType.substringAfter('/', "").uppercase()
        }.takeIf { it.isNotBlank() }
    val kbps = (bitrate / 1000).takeIf { it > 0 }?.let { "$it kbps" }
    return listOfNotNull(codec, kbps).joinToString(" · ").ifEmpty { null }
}
