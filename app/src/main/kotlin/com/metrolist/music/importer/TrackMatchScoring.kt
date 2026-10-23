/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** What the matcher needs to know about one YouTube Music search result. */
data class MatchCandidate(
    val title: String,
    val artists: List<String>,
    val durationSec: Int?,
    val isVideo: Boolean = false,
)

/**
 * Scores how likely a YouTube Music result is the same recording as an imported track.
 * Pure functions so the weighting can be unit tested.
 */
object TrackMatchScoring {
    /** Results scoring below this are reported as "not found". */
    const val MATCH_THRESHOLD = 0.6

    private val versionWords =
        setOf(
            "live", "remix", "acoustic", "instrumental", "karaoke", "cover", "sped", "slowed",
            "reverb", "nightcore", "8d", "demo", "unplugged", "edit", "mix", "version", "lofi",
        )

    // Qualifiers that don't change which recording it is.
    private val neutralQualifier =
        Regex("\\b(feat|ft|featuring|with|prod|remaster(ed)?|\\d{4} remaster(ed)?|official|audio|video|lyrics?|explicit|clean|radio edit|single|mono|stereo)\\b")

    fun score(track: ImportTrack, candidate: MatchCandidate): Double {
        val title = titleSimilarity(track.title, candidate.title)
        val artist = artistSimilarity(track.artists, candidate.artists)
        val duration = durationSimilarity(track.durationMs?.let { (it / 1000).toInt() }, candidate.durationSec)

        var score = 0.5 * title + 0.35 * (artist ?: 0.5) + 0.15 * duration
        if (title < 0.5) score *= 0.5
        if (artist == 0.0) score *= 0.6
        if (versionTags(track.title) != versionTags(candidate.title)) score -= 0.15
        if (candidate.isVideo) score -= 0.05
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Index of the best candidate and its score, or null when nothing reaches [MATCH_THRESHOLD].
     * Earlier search results win ties and get a small boost, since YouTube's own ranking is useful.
     */
    fun best(track: ImportTrack, candidates: List<MatchCandidate>): Pair<Int, Double>? =
        candidates
            .mapIndexed { index, candidate ->
                val rankBonus = when (index) { 0 -> 0.03; 1 -> 0.015; else -> 0.0 }
                index to (score(track, candidate) + rankBonus).coerceAtMost(1.0)
            }.maxWithOrNull(compareBy<Pair<Int, Double>> { it.second }.thenByDescending { it.first })
            ?.takeIf { it.second >= MATCH_THRESHOLD }

    fun titleSimilarity(a: String, b: String): Double {
        val coreA = coreTitle(a)
        val coreB = coreTitle(b)
        if (coreA.isEmpty() || coreB.isEmpty()) return 0.0
        if (coreA == coreB) return 1.0
        return maxOf(
            dice(tokens(coreA), tokens(coreB)),
            dice(bigrams(coreA.replace(" ", "")), bigrams(coreB.replace(" ", ""))),
        )
    }

    /** null when the imported track has no artist information. */
    fun artistSimilarity(source: List<String>, candidate: List<String>): Double? {
        val wanted = source.map(::fold).filter { it.isNotEmpty() }
        if (wanted.isEmpty()) return null
        val found = candidate.map(::fold).filter { it.isNotEmpty() }
        if (found.isEmpty()) return 0.0
        val joined = found.joinToString(" ")

        fun hit(name: String) =
            joined.contains(name) ||
                found.any { it.contains(name) || name.contains(it) || dice(bigrams(it), bigrams(name)) >= 0.8 }

        val primary = if (hit(wanted.first())) 1.0 else 0.0
        if (wanted.size == 1) return primary
        val others = wanted.drop(1).count(::hit).toDouble() / (wanted.size - 1)
        return if (primary == 0.0 && others == 0.0) 0.0 else 0.75 * primary + 0.25 * others
    }

    fun durationSimilarity(aSec: Int?, bSec: Int?): Double {
        if (aSec == null || bSec == null || aSec <= 0 || bSec <= 0) return 0.5
        val diff = abs(aSec - bSec)
        return when {
            diff <= 2 -> 1.0
            diff <= 5 -> 0.85
            diff <= 10 -> 0.6
            diff <= 20 -> 0.3
            else -> 0.0
        }
    }

    /** Title without bracketed parts, " - Remastered 2011"-style suffixes, featuring credits and punctuation. */
    fun coreTitle(title: String): String {
        var t = fold(title)
        t = t.replace(Regex("[(\\[{].*?[)\\]}]"), " ")
        t = t.substringBefore(" - ")
        t = t.replace(Regex("\\b(feat|ft|featuring)\\b.*$"), " ")
        return t.replace(NON_WORD, " ").trim()
    }

    private fun versionTags(title: String): Set<String> {
        val folded = fold(title).replace(neutralQualifier, " ")
        return tokens(folded.replace(NON_WORD, " ")).filter { it in versionWords }.toSet()
    }

    // Only Latin-style accents; Indic and other scripts keep their vowel signs.
    private val COMBINING_ACCENTS = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val NON_WORD = Regex("[^\\p{L}\\p{M}\\p{N}]+")

    fun fold(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(COMBINING_ACCENTS, "")
            .lowercase(Locale.ROOT)
            .replace('&', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun tokens(text: String): List<String> = text.split(' ').filter { it.isNotEmpty() }

    private fun bigrams(text: String): List<String> =
        if (text.length < 2) listOf(text) else (0 until text.length - 1).map { text.substring(it, it + 2) }

    /** Sørensen–Dice coefficient over multisets. */
    private fun dice(a: List<String>, b: List<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val counts = HashMap<String, Int>()
        a.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        var overlap = 0
        b.forEach { token ->
            val left = counts[token] ?: 0
            if (left > 0) {
                overlap++
                counts[token] = left - 1
            }
        }
        return 2.0 * overlap / (a.size + b.size)
    }
}
