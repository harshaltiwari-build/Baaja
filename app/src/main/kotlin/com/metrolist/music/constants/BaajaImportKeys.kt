/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Spotify developer keys saved by earlier builds; no longer used, but still stripped from backups. */
val SpotifyClientIdKey = stringPreferencesKey("baaja_spotify_client_id")
val SpotifyClientSecretKey = stringPreferencesKey("baaja_spotify_client_secret")

/** Local music screen: last chosen sort order. */
val LocalMusicSortKey = stringPreferencesKey("baaja_local_music_sort")
val LocalMusicSortDescendingKey = booleanPreferencesKey("baaja_local_music_sort_desc")
