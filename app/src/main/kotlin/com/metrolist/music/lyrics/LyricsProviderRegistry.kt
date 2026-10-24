/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "BetterLyrics" to BetterLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
        "LrcLib" to LrcLibLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "LyricsPlus" to LyricsPlusProvider,
        "Zemer" to ZemerLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTube" to YouTubeLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    // Fixed fallback chain; LrcLib is tried first.
    fun getDefaultProviderOrder(): List<String> = listOf(
        "LrcLib",
        "BetterLyrics",
        "KuGou",
        "Paxsenix",
        "LyricsPlus",
        "Zemer",
        "YouTubeSubtitle",
        "YouTube",
    )

    fun getFixedProviders(): List<LyricsProvider> =
        getDefaultProviderOrder().mapNotNull { getProviderByName(it) }
}
