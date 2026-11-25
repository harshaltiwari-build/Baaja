/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

/** Shows the current synced lyric line under the artwork on Now Playing (default on). */
val ShowLyricPreviewOnPlayerKey = booleanPreferencesKey("baajaShowLyricPreviewOnPlayer")


/** Multiplier on the lyrics style's own text size (default 1). */
val LyricsTextScaleKey = floatPreferencesKey("baajaLyricsTextScale")

/** Multiplier on the lyrics style's own line spacing (default 1). */
val LyricsLineSpacingScaleKey = floatPreferencesKey("baajaLyricsLineSpacingScale")

/** The Song | Video switch in the player (default on). */
val MusicVideosEnabledKey = booleanPreferencesKey("baajaMusicVideosEnabled")

/** Highest music video resolution, in lines (default 720). */
val MusicVideoQualityKey = intPreferencesKey("baajaMusicVideoQuality")

/** Open the player in Video for songs that have an official music video (default off). */
val MusicVideoAutoStartKey = booleanPreferencesKey("baajaMusicVideoAutoStart")

val MusicVideoQualities = listOf(360, 480, 720, 1080)
