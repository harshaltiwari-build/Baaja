package com.metrolist.music.recommend

import java.text.Normalizer

/**
 * Tells uploads of the same song apart from different songs, so autoplay doesn't play
 * "Song", then "Song (Official Video)", then "Song - Lyrics" back to back.
 *
 * Two tracks count as the same song when their titles match once upload dressing
 * ("Official Video", "Lyrics", "feat. X", "Artist - ") is removed and they share the main artist.
 */
object SongIdentity {
    private val BRACKETS = Regex("""\s*[(\[【{]([^)\]】}]*)[)\]】}]""")
    private val FEATURING = Regex("""\s+(feat\.?|ft\.?|featuring|prod\.?)\s+.*$""", RegexOption.IGNORE_CASE)
    private val UPLOAD_WORDS =
        Regex(
            """\b(official|music|lyric|lyrics|lyrical|video|audio|visuali[sz]er|hd|hq|4k|full|song|mv|explicit|clean|remaster(ed)?)\b""",
            RegexOption.IGNORE_CASE,
        )
    private val DASH = Regex("""\s+[-–—|]\s+""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
    private val MARKS = Regex("""\p{M}+""")

    fun key(
        title: String,
        artist: String?,
    ): String {
        var t = fold(title)
        val main = artist?.let(::mainArtist).orEmpty()
        val parts = DASH.split(t)
        if (parts.size >= 2) {
            // "Artist - Title": keep the part that isn't the artist.
            t = parts.firstOrNull { clean(it).isNotEmpty() && clean(it) != main } ?: parts.first()
        }
        return "${clean(t)}|$main"
    }

    fun mainArtist(artist: String): String = fold(artist.split(",", "&", " x ", " and ").first()).let(::clean)

    private fun clean(s: String): String {
        var t = s.replace(BRACKETS, " ")
        t = FEATURING.replace(t, "")
        t = UPLOAD_WORDS.replace(t, " ")
        return t.replace(NON_WORD, " ").trim()
    }

    private fun fold(s: String): String =
        Normalizer
            .normalize(s, Normalizer.Form.NFKD)
            .replace(MARKS, "")
            .lowercase()
}
